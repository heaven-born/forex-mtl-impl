package forex.services.rates

import cats.Applicative
import cats.data.EitherT
import cats.implicits._
import forex.OneFrameStateDomain.OneFrameRate
import forex.services.rates.errors.OneFrameServiceError
import forex.services.rates.errors.OneFrameServiceError.{JsonMappingError, JsonParsingError}
import io.circe.parser.parse
import io.circe.generic.auto._

object OneFrameJsonMapper {

  def jsonToRates[F[_]:Applicative](json: String): EitherT[F,OneFrameServiceError,List[OneFrameRate]] = {

      val either:Either[OneFrameServiceError, List[OneFrameRate]] =  for {
        json <- parse(json)
          .leftMap(e => JsonParsingError("Parsing error", e))
        obj <- json.as[List[OneFrameRate]]
          .leftMap(e => JsonMappingError(s"Mapping error. Reason: ${e.getMessage()}. JSON: ${json.noSpaces}", e))
      } yield obj

      EitherT(either.pure[F])

  }

}
