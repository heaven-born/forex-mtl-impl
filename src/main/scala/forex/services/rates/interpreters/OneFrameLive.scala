package forex.services.rates.interpreters


import java.util.concurrent.TimeUnit

import cats.Applicative
import cats.data.EitherT
import cats.effect.concurrent.Ref
import cats.effect.{Async, Timer}
import forex.config.OneFrameServerHttpConfig
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.services.rates.OneFrameHttpRequestHandler
import forex.services.rates.errors._
import cats.implicits._
import com.typesafe.scalalogging.Logger
import forex.OneFrameStateDomain.{OneFrameRate, OneFrameRateState}
import forex.services.rates
import forex.services.rates.errors.RatesServiceError.{OneFrameLookupJsonMappingError, OneFrameLookupJsonParsingError}
import forex.state.Schedulable
import fs2.Stream
import io.circe.generic.auto._
import io.circe.parser._

import scala.concurrent.duration.FiniteDuration




class OneFrameLive[F[_]: Applicative : Async: Timer](
                     config: OneFrameServerHttpConfig,
                     requestHandler: OneFrameHttpRequestHandler[F],
                     oneFrameState: Ref[F, Option[OneFrameRateState]])
  extends rates.Algebra[F] with Schedulable[F] {

  val logger = Logger[OneFrameLive[F]]


  override def get(pair: Rate.Pair): F[RatesServiceError Either Rate] = {
    val r = for {
      json <- requestHandler.getFreshData()
        _ = logger.info("Json: "+json)
      rate <- mapJsonToRates(json)
        _ = logger.info("Rate: "+rate)
        fromCurrency = Currency.fromString(rate(0).from) // FIX: remove  index
        toCurrency = Currency.fromString(rate(0).to)
        price = rate(0).price
        timestamp = rate(0).time_stamp
    } yield Rate(Rate.Pair(fromCurrency,toCurrency), Price(price), Timestamp(timestamp))

    r.value

  }

  override def scheduledTasks: Stream[F, Unit] = {
    def t:F[Unit] = Timer[F].sleep(FiniteDuration(10, TimeUnit.SECONDS)) >> {
      Applicative[F].pure(println("tick")) >> t
    }

    Stream.eval(t)
  }

  private def mapJsonToRates(json: String): EitherT[F,RatesServiceError, List[OneFrameRate]] = {

    val either:Either[RatesServiceError, List[OneFrameRate]] = for {
      json <- parse(json)
        .leftMap(e => OneFrameLookupJsonParsingError("Parsing error", Some(e)))
      obj <- json.as[List[OneFrameRate]]
        .leftMap(e => OneFrameLookupJsonMappingError(s"Mapping error. Reason: ${e.getMessage()}. JSON: ${json.noSpaces}", Some(e)))
    } yield obj

    EitherT(either.pure[F])
  }




}

