package forex.services.rates

import cats.effect.{Async, Timer}
import forex.OneFrameStateDomain.OneFrameStateRef
import forex.config.RatesService
import interpreters._

object Interpreters {
  def live[F[_]: Timer: Async](config: RatesService,
                               oneFrameState: OneFrameStateRef[F]) = {

    val cacheProcessor = new OneFrameCacheProcessor(oneFrameState)
    val handler = OneFrameHttpRequestHandler[F](config.oneFrameServerHttp)
    new OneFrameLive[F](
      config,
      handler,
      cacheProcessor)
  }
}
