package forex.services.rates

import cats.Applicative
import cats.effect.concurrent.Ref
import cats.effect.{Async, Timer}
import forex.OneFrameStateDomain.OneFrameRateState
import forex.config.OneFrameServerHttpConfig
import interpreters._

object Interpreters {
  def dummy[F[_]: Applicative](): Algebra[F] = new OneFrameDummy[F]()
  def live[F[_]: Applicative: Async: Timer](
         config: OneFrameServerHttpConfig,
         oneFrameState: Ref[F, Option[OneFrameRateState]]): Algebra[F] =
    new OneFrameLive[F](config, new OneFrameHttpRequestHandler[F](config.url), oneFrameState)
}
