package forex.services.rates

import cats.Applicative
import cats.effect.{Async, Concurrent, Timer}
import forex.OneFrameStateDomain.OneFrameState
import forex.config.RatesService
import interpreters._

object Interpreters {
  def live[F[_]: Applicative: Async: Timer: Concurrent](
                                                         config: RatesService,
                                                         oneFrameState: OneFrameState[F]) = {

    val cacheProcessor = OneFrameCacheProcessor(oneFrameState)
    val handler = OneFrameHttpRequestHandler[F](config.oneFrameServerHttp)
    new OneFrameLive[F](
      config,
      handler,
      cacheProcessor)
  }
}
