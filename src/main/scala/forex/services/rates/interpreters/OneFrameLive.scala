package forex.services.rates.interpreters

import java.time.OffsetDateTime

import cats.Applicative
import cats.data.EitherT
import cats.effect.Async
import forex.config.OneFrameServerHttpConfig
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.services.rates.Algebra
import forex.services.rates.errors._
import sttp.client._
import cats.implicits._
import forex.services.rates.errors.RatesServiceError.{OneFrameLookupConnectionError, OneFrameLookupJsonMappingError, OneFrameLookupJsonParsingError, OneFrameLookupResponseError}
import forex.services.rates.interpreters.OneFrameLive.OneFrameRate
import io.circe.generic.auto._
import io.circe.parser._

import scala.util.{Failure, Success, Try}


class OneFrameLive[F[_]: Applicative : Async](config: OneFrameServerHttpConfig) extends Algebra[F] {

  implicit val sttpBackend = HttpURLConnectionBackend()

  override def get(pair: Rate.Pair): F[RatesServiceError Either Rate] = {
    val request = basicRequest
        .get(uri"http://localhost:8081/rates?pair=${pair.from}${pair.to}")
        .header("token","10dc303535874aeccc86a8251e6992f5")
    val res  = Async[F].delay(Try(request.send()))

    val r = for {
      json <- handleEndpointErrors(res)
      rate <- handleJsonErrors(json)
        fromCurrency = Currency.fromString(rate(0).from) // FIX: remove  index
        toCurrency = Currency.fromString(rate(0).to)
        price = rate(0).price
        timestamp = rate(0).time_stamp
    } yield Rate(Rate.Pair(fromCurrency,toCurrency), Price(price), Timestamp(timestamp))

    r.value

  }

  private def handleJsonErrors(json: String): EitherT[F,RatesServiceError, List[OneFrameRate]] = {

    val either:Either[RatesServiceError, List[OneFrameRate]] = for {
      json <- parse(json)
        .leftMap(e => OneFrameLookupJsonParsingError("Parsing error", Some(e)))
      obj <- json.as[List[OneFrameRate]]
        .leftMap(e => OneFrameLookupJsonMappingError(s"Mapping error. Reason: ${e.getMessage()}. JSON: ${json.noSpaces}", Some(e)))
    } yield obj

    EitherT(either.pure[F])
  }


  private def handleEndpointErrors(
              res: F[Try[Response[Either[String, String]]]]): EitherT[F, RatesServiceError, String] =
    EitherT {
      res.map {
        case Failure(ex) =>
          OneFrameLookupConnectionError("Can't get rates from data provider", Some(ex)).asLeft[String]
        case Success(resp) => resp.body.leftMap(
          OneFrameLookupResponseError(_)
        )
      }
    }


  //allSupportedCurrencies.combinations(2) .flatMap(_.permutations) .map(p=>"pair="+p(0)+p(1)+"&")

}

object OneFrameLive {
  case class OneFrameRate (from: String,
                           to: String,
                           bid: BigDecimal ,
                           ask: BigDecimal,
                           price: BigDecimal,
                           time_stamp: OffsetDateTime)

}
