package forex.services.rates

import cats.Applicative
import cats.effect.{Async, Timer}
import forex.OneFrameStateDomain.OneFrameStateRef
import forex.config.ApplicationConfig
import forex.services.rates.interpreters.{OneFrame, OneFrameCacheProcessor, OneFrameHttpRequestHandler}


class LiveInstances[F[_]: Async: Applicative: Timer]
      (config: ApplicationConfig, sharedState: OneFrameStateRef[F]) {

  implicit val conf = config
  implicit val oneFrameCache = sharedState

  implicit val oneFrameHttpRequestHandler =  new OneFrameHttpRequestHandler[F]
  implicit val oneFrameCacheProcessor = new OneFrameCacheProcessor[F]()
  implicit val oneFrame = new OneFrame[F]()

}
