package forex.services.rates.interpreters


import cats.data.EitherT
import cats.effect.{Async, Timer}
import forex.config.ApplicationConfig
import forex.domain.Rate
import forex.services.rates.{CacheDomainConverter, OneFrameAlgebra, OneFrameCacheProcessor, OneFrameCacheProcessorAlgebra, OneFrameHttpRequestHandler, OneFrameHttpRequestHandlerAlgebra, OneFrameJsonMapper}
import forex.services.rates.errors._
import cats.implicits._
import com.typesafe.scalalogging.Logger
import forex.OneFrameStateDomain.OneFrameRate
import forex.state.Schedulable
import fs2.Stream

class OneFrame[F[_]: Timer: Async: OneFrameCacheProcessorAlgebra: OneFrameHttpRequestHandlerAlgebra]
  (implicit config: ApplicationConfig)
    extends OneFrameAlgebra[F] with Schedulable[F] {

  private val logger = Logger[OneFrame[F]]

  override def get(pair: Rate.Pair): F[OneFrameServiceError Either Rate] = {
    val currencyPair = CacheDomainConverter.convert(pair)

    val r = for {
      cacheRate <- OneFrameCacheProcessor[F].getCurrencyPair(currencyPair)
      _ = logger.info(s"Cache rate: ${cacheRate}")
      domainRate = CacheDomainConverter.convert(cacheRate)
    } yield  domainRate


    r.value

  }

  override def scheduledTasks: Stream[F, Unit] = {

    def getRatesFromOneFrame():EitherT[F,OneFrameServiceError, List[OneFrameRate]] = for {
      json <- OneFrameHttpRequestHandler[F].getFreshData
      _ = logger.debug(s"Json: $json")
      rate <- OneFrameJsonMapper.jsonToRates[F](json)
      _ = logger.debug(s"Rates: $rate")
    } yield  rate


    def cacheUpdate:F[Unit] = getRatesFromOneFrame().value.flatMap {
      case Left(error) =>
        val setNewState = OneFrameCacheProcessor[F].updateWithError(error)
        setNewState >> Timer[F].sleep(config.ratesService.ratesRequestRetryInterval) >> cacheUpdate
      case Right(data) =>
        val deadline = config.ratesService.cacheExpirationTime.fromNow
        val setNewState = OneFrameCacheProcessor[F].setData(deadline,data)
        setNewState >> Timer[F].sleep(config.ratesService.ratesRequestInterval) >> cacheUpdate
    }

    Stream.eval(cacheUpdate)
  }


}

object OneFrame {
  implicit def apply[F[_]: Async: OneFrameAlgebra] =
    implicitly[OneFrameAlgebra[F]]
}

