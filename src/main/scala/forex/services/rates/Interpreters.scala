package forex.services.rates

import cats.Applicative
import cats.effect.{Async, Timer}
import forex.config.OneFrameServerHttpConfig
import interpreters._

object Interpreters {
  def dummy[F[_]: Applicative](): Algebra[F] = new OneFrameDummy[F]()
  def live[F[_]: Applicative: Async: Timer](config: OneFrameServerHttpConfig): Algebra[F] =
    new OneFrameLive[F](config, new OneFrameHttpRequestHandler[F](config.url))
}
