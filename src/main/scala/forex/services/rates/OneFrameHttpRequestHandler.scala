package forex.services.rates

import cats.Applicative
import cats.data.EitherT
import cats.effect.Async
import forex.domain.Currency.allSupportedCurrencies
import forex.services.rates.errors.RatesServiceError
import forex.services.rates.errors.RatesServiceError.{OneFrameLookupConnectionError, OneFrameLookupResponseError}
import sttp.client.{HttpURLConnectionBackend, basicRequest}
import sttp.client._
import cats.data.EitherT
import cats.implicits._
import com.typesafe.scalalogging.Logger

import scala.util.{Failure, Success, Try}

class OneFrameHttpRequestHandler[F[_]: Applicative : Async] (url:String){

  val logger = Logger[OneFrameHttpRequestHandler[F]]

  implicit val sttpBackend = HttpURLConnectionBackend()

  def getFreshData() = {

      val pairs = allSupportedCurrencies.toList
        .combinations(2)
        .flatMap(_.permutations)
        .map(p=>"pair"->s"${p.head}${p(1)}").toMap

      val request = basicRequest
        .get(uri"$url/rates?$pairs")
        .header("token","10dc303535874aeccc86a8251e6992f5")

    val rawResponse = Async[F].delay(Try(request.send()))
    convertEndpointErrorsToServiceErrors(rawResponse)
  }

  private def convertEndpointErrorsToServiceErrors(
                  res: F[Try[Response[Either[String, String]]]]): EitherT[F, RatesServiceError, String] =
    EitherT {
      res.map {
        case Failure(ex) =>
          OneFrameLookupConnectionError("Can't get rates from data provider", Some(ex))
            .asLeft[String]
        case Success(resp) => resp.body.leftMap(
          OneFrameLookupResponseError(_)
        )
      }
    }



}
