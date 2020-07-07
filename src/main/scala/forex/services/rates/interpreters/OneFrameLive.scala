package forex.services.rates.interpreters


import cats.data.EitherT
import cats.effect.{Async,Timer}
import forex.config.RatesService
import forex.domain.Rate
import forex.services.rates.{CacheDomainConverter, OneFrameCacheProcessor, OneFrameHttpRequestHandler, OneFrameJsonMapper}
import forex.services.rates.errors._
import cats.implicits._
import com.typesafe.scalalogging.Logger
import forex.OneFrameStateDomain.OneFrameRate
import forex.services.rates
import forex.state.Schedulable
import fs2.Stream


class OneFrameLive[F[_]: Timer: Async](config: RatesService,
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

    def getRatesFromOneFrame():EitherT[F,OneFrameServiceError, List[OneFrameRate]] = for {
      json <- requestHandler.getFreshData()
      _ = logger.debug(s"Json: $json")
      rate <- OneFrameJsonMapper.jsonToRates(json)
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


}

