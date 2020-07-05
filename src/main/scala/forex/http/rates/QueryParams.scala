package forex.http.rates

import cats.implicits.catsSyntaxValidatedId
import forex.domain.Currency
import org.http4s.{ParseFailure, QueryParamDecoder, QueryParameterValue}
import org.http4s.dsl.impl.ValidatingQueryParamDecoderMatcher

import scala.util.{Failure, Success, Try}

object QueryParams {

  implicit lazy val stringQueryParamDecoder: QueryParamDecoder[Currency] =
    (value: QueryParameterValue ) => Try(Currency.fromString(value.value)) match {
      case Success(v) => v.validNel[ParseFailure]
      case Failure(_) => ParseFailure(
              "Failed to parse Currency query parameter",
               s"Could not parse ${value.value} as a Currency")
        .invalidNel[Currency]
    }

  object FromQueryParam extends ValidatingQueryParamDecoderMatcher[Currency]("from")
  object ToQueryParam extends ValidatingQueryParamDecoderMatcher[Currency]("to")

}
