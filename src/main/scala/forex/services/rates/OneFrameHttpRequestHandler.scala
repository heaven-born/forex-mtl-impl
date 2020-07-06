package forex.services.rates


import cats.Applicative
import cats.effect.Async
import forex.domain.Currency.allSupportedCurrencies
import forex.services.rates.errors.OneFrameServiceError
import forex.services.rates.errors.OneFrameServiceError.{LookupConnectionError, LookupResponseError}
import sttp.client.{HttpURLConnectionBackend, basicRequest}
import sttp.client._
import cats.data.EitherT
import cats.implicits._
import com.typesafe.scalalogging.Logger
import forex.config.OneFrameServerHttpConfig

import scala.util.{Failure, Success, Try}

case class OneFrameHttpRequestHandler[F[_]: Applicative : Async] (config:OneFrameServerHttpConfig){

  val logger = Logger[OneFrameHttpRequestHandler[F]]

  implicit val sttpBackend = HttpURLConnectionBackend()

  def getFreshData():EitherT[F,OneFrameServiceError, String] = {

      val pairs = allSupportedCurrencies.toList
        .combinations(2)
        .flatMap(_.permutations)
        .map(p=>"pair"->s"${p.head}${p(1)}").toSeq

      val requestUrl = uri"${config.url}/rates?$pairs"
      logger.info(s"Request url: $requestUrl")

      val request = basicRequest
        .get(requestUrl)
        .header("token","10dc303535874aeccc86a8251e6992f5")
        .readTimeout(config.timeout)

    val rawResponse = Async[F].delay(Try(request.send()))
    convertEndpointErrorsToServiceErrors(rawResponse)
  }

  private def convertEndpointErrorsToServiceErrors(
                  res: F[Try[Response[Either[String, String]]]]): EitherT[F, OneFrameServiceError, String] =
    EitherT {
      res.map {
        case Failure(ex) =>
          LookupConnectionError("Can't get rates from data provider", Some(ex))
            .asLeft[String]
        case Success(resp) => resp.body.leftMap(
          LookupResponseError
        )
      }
    }



}
