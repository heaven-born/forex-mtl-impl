package forex.services.rates

import cats.Applicative
import cats.effect.{Async, Concurrent, Timer}
import forex.OneFrameStateDomain.OneFrameState
import forex.config.RatesService
import interpreters._

object Interpreters {
  def live[F[_]: Applicative: Async: Timer: Concurrent](
                                                         config: RatesService,
                                                         oneFrameState: OneFrameState[F]) =
    new OneFrameLive[F](
      config,
      new OneFrameHttpRequestHandler[F](config.oneFrameServerHttp),
      oneFrameState)
}
