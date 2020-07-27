package forex.services.rates.interpreters

import cats.Applicative
import cats.data.EitherT
import cats.implicits._
import com.typesafe.scalalogging.Logger
import forex.config.ApplicationConfig
import forex.domain.Currency.allSupportedCurrencies
import forex.services.rates.OneFrameHttpRequestHandlerAlgebra
import forex.services.rates.errors.OneFrameServiceError
import forex.services.rates.errors.OneFrameServiceError.{LookupConnectionError, LookupResponseError}
import sttp.client.{HttpURLConnectionBackend, basicRequest, _}

import scala.util.{Failure, Success, Try}


class OneFrameHttpRequestHandler[F[_]: Applicative] (implicit config:ApplicationConfig)
  extends OneFrameHttpRequestHandlerAlgebra[F] {

  val logger = Logger[OneFrameHttpRequestHandler[F]]

  implicit val sttpBackend = HttpURLConnectionBackend()

  def getFreshData:EitherT[F,OneFrameServiceError, String] = {

    val pairs = allSupportedCurrencies.toList
      .combinations(2)
      .flatMap(_.permutations)
      .map(p=>"pair"->s"${p.head}${p(1)}").toSeq

    val requestUrl = uri"${config.ratesService.oneFrameServerHttp.url}/rates?$pairs"
    logger.info(s"Request url: $requestUrl")

    val request = basicRequest
      .get(requestUrl)
      .header("token","10dc303535874aeccc86a8251e6992f5")
      .readTimeout(config.ratesService.oneFrameServerHttp.timeout)

    val rawResponse = Try(request.send()).pure[F]
    convertEndpointErrorsToServiceErrors(rawResponse)
  }

  private def convertEndpointErrorsToServiceErrors(
                          res: F[Try[Response[Either[String, String]]]]): EitherT[F, OneFrameServiceError, String] =
    EitherT {
      res.map {
        case Failure(ex) =>
          LookupConnectionError("Can't get rates from data provider", ex)
            .asLeft[String]
        case Success(resp) => resp.body.leftMap(
          LookupResponseError
        )
      }
    }



}

object OneFrameHttpRequestHandler {
  implicit def apply[F[_]:Applicative: OneFrameHttpRequestHandlerAlgebra] =
    implicitly[OneFrameHttpRequestHandlerAlgebra[F]]

}
