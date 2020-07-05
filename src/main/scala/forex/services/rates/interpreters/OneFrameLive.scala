package forex.services.rates.interpreters


import cats.Applicative
import cats.data.EitherT
import cats.effect.{Async, Concurrent, Timer}
import forex.config.RatesService
import forex.domain.Rate
import forex.services.rates.{CacheDomainConverter, OneFrameHttpRequestHandler}
import forex.services.rates.errors._
import cats.implicits._
import com.typesafe.scalalogging.Logger
import forex.OneFrameStateDomain.{CurrencyPair, OneFrameRate, OneFrameRateState, OneFrameState}
import forex.services.rates
import forex.services.rates.errors.OneFrameServiceError.{JsonMappingError, JsonParsingError, NoSuchCurrencyPairError}
import forex.state.Schedulable
import forex.utils.TimeUtils
import fs2.Stream
import io.circe.generic.auto._
import io.circe.parser._


class OneFrameLive[F[_]: Applicative : Async: Timer: Concurrent](config: RatesService,
                                                                 requestHandler: OneFrameHttpRequestHandler[F],
                                                                 oneFrameCache: OneFrameState[F])
  extends rates.Algebra[F] with Schedulable[F] {

  private val logger = Logger[OneFrameLive[F]]


  override def get(pair: Rate.Pair): F[OneFrameServiceError Either Rate] = {
    val currencyPair = CacheDomainConverter.convert(pair)

    def currencyPairValidation(state: OneFrameRateState) =
      if (state.rates.contains(currencyPair)) Right(state)
      else Left(NoSuchCurrencyPairError(s"${pair.from}/${pair.to}"):OneFrameServiceError)

    val r = for {
      state <- EitherT(oneFrameCache.get)
      _     <- EitherT(currencyPairValidation(state).pure[F])
      cacheRate = state.rates(currencyPair)
      _ = logger.info(s"Cache rate: ${cacheRate}")
      domainRate = CacheDomainConverter.convert(cacheRate)
    } yield domainRate

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
        logger.error(error.msg)
        val setNewState = oneFrameCache.update {
            case Left(_) =>
              logger.error(s"Cache was updated with the last error: ${error.msg}")
              Left(error)
            case r @ Right(cache) =>
              if (cache.expiredOn.isOverdue()) {
                logger.error("Cache was expired and removed.")
                Left(error)
              } else r
        }
        setNewState >> Timer[F].sleep(config.ratesRequestRetryInterval) >> cacheUpdate
      case Right(data) =>
        val newRates = data.map(d => CurrencyPair(d.from,d.to) -> d).toMap
        val deadline = config.cacheExpirationTime.fromNow
        logger.info(s"Received new rates with expiration time ${TimeUtils.deadlineToDate(deadline)} for currency paris: ${newRates.keys.mkString(" ")}")
        val setNewState = oneFrameCache.set(Right(OneFrameRateState(deadline,newRates)))
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

