package forex.services.rates.interpreters


import java.util.concurrent.TimeUnit

import cats.Applicative
import cats.data.EitherT
import cats.effect.{Async, Concurrent, Timer}
import forex.config.RatesService
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.services.rates.OneFrameHttpRequestHandler
import forex.services.rates.errors._
import cats.implicits._
import com.typesafe.scalalogging.Logger
import forex.OneFrameStateDomain.{CurrencyPair, OneFrameRate, OneFrameRateState, OneFrameState}
import forex.services.rates
import forex.services.rates.errors.OneFrameServiceError.{JsonMappingError, JsonParsingError}
import forex.state.Schedulable
import fs2.Stream
import io.circe.generic.auto._
import io.circe.parser._

import scala.concurrent.duration.FiniteDuration




class OneFrameLive[F[_]: Applicative : Async: Timer: Concurrent](config: RatesService,
                                                                 requestHandler: OneFrameHttpRequestHandler[F],
                                                                 oneFrameCache: OneFrameState[F])
  extends rates.Algebra[F] with Schedulable[F] {

  val logger = Logger[OneFrameLive[F]]


  override def get(pair: Rate.Pair): F[OneFrameServiceError Either Rate] = {
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

    val rates = for {
      json <- requestHandler.getFreshData()
      _ = logger.debug(s"Json: ${json}")
      rate <- mapJsonToRates(json)
      _ = logger.debug(s"Rates: ${rate}")
    } yield  rate


    def cacheUpdate:F[Unit] = rates.value.flatMap {
      case Left(error) =>
        logger.error(error.msg, error.ex.getOrElse("Type: Application Error"))
        oneFrameCache.update {
            case Left(err) =>
              logger.error(s"Cache was updated with the last error: ${err.msg}")
              Left(error)
            case r @ Right(cache) =>
              logger.error("Cache was leaned due to expiration time.")
              if (cache.expiredOn.isOverdue()) Left(error) else r
        }
        Timer[F].sleep(config.ratesRequestRetryInterval) >> cacheUpdate
      case Right(data) =>
        val newRates = data.map(d => (CurrencyPair(d.from,d.to) -> d)).toMap
        val deadline = FiniteDuration(5, TimeUnit.MINUTES).fromNow
        logger.info(s"Received new rates with deadline ${deadline} for currency paris: ${newRates.keys}")
        oneFrameCache.set(Right(OneFrameRateState(deadline,newRates)))
        Timer[F].sleep(config.ratesRequestInterval) >> cacheUpdate
    }

    Stream.eval(cacheUpdate)
  }

  private def mapJsonToRates(json: String): EitherT[F,OneFrameServiceError, List[OneFrameRate]] = {

    val either:Either[OneFrameServiceError, List[OneFrameRate]] = for {
      json <- parse(json)
        .leftMap(e => JsonParsingError("Parsing error", Some(e)))
      obj <- json.as[List[OneFrameRate]]
        .leftMap(e => JsonMappingError(s"Mapping error. Reason: ${e.getMessage()}. JSON: ${json.noSpaces}", Some(e)))
    } yield obj

    EitherT(either.pure[F])
  }




}

