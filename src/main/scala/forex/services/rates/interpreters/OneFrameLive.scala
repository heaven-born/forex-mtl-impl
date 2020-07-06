package forex.services.rates.interpreters


import cats.Applicative
import cats.data.EitherT
import cats.effect.{Async, Concurrent, Timer}
import forex.config.RatesService
import forex.domain.Rate
import forex.services.rates.{CacheDomainConverter, OneFrameCacheProcessor, OneFrameHttpRequestHandler}
import forex.services.rates.errors._
import cats.implicits._
import com.typesafe.scalalogging.Logger
import forex.OneFrameStateDomain.OneFrameRate
import forex.services.rates
import forex.services.rates.errors.OneFrameServiceError.{JsonMappingError, JsonParsingError}
import forex.state.Schedulable
import fs2.Stream
import io.circe.generic.auto._
import io.circe.parser._


class OneFrameLive[F[_]: Applicative : Async: Timer: Concurrent](config: RatesService,
                                                                 requestHandler: OneFrameHttpRequestHandler[F],
                                                                 cache: OneFrameCacheProcessor[F])
  extends rates.Algebra[F] with Schedulable[F] {

  private val logger = Logger[OneFrameLive[F]]


  override def get(pair: Rate.Pair): F[OneFrameServiceError Either Rate] = {
    val currencyPair = CacheDomainConverter.convert(pair)

    val r = for {
      cacheRate <- cache.getCurrencyPair(currencyPair)
      _ = logger.info(s"Cache rate: ${cacheRate}")
      domainRate = CacheDomainConverter.convert(cacheRate)
    } yield  domainRate


    r.value

  }

  override def scheduledTasks: Stream[F, Unit] = {

    def getRatesFromOneFrame() = for {
      json <- requestHandler.getFreshData()
      _ = logger.debug(s"Json: $json")
      rate <- mapJsonToRates(json)
      _ = logger.debug(s"Rates: $rate")
    } yield  rate


    def cacheUpdate:F[Unit] = getRatesFromOneFrame().value.flatMap {
      case Left(error) =>
        val setNewState = cache.updateWithError(error)
        setNewState >> Timer[F].sleep(config.ratesRequestRetryInterval) >> cacheUpdate
      case Right(data) =>
        val deadline = config.cacheExpirationTime.fromNow
        val setNewState = cache.setData(deadline,data)
        setNewState >> Timer[F].sleep(config.ratesRequestInterval) >> cacheUpdate
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

