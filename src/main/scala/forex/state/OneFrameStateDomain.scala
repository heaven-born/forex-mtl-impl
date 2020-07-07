package forex

import java.time.OffsetDateTime

import cats.effect.Sync
import cats.effect.concurrent.Ref
import forex.services.rates.errors.OneFrameServiceError
import forex.services.rates.errors.OneFrameServiceError.StateInitializationError

import scala.concurrent.duration.Deadline

object OneFrameStateDomain {

  type OneFrameStateRef[F[_]] = Ref[F, OneFrameState[F]]
  type OneFrameState[F[_]] = Either[OneFrameServiceError,OneFrameRateStateHolder]

  def init[F[_]:Sync]:F[OneFrameStateRef[F]] = Ref.of(Left(StateInitializationError()))

  case class CurrencyPair(from: String, to: String)

  case class OneFrameRateStateHolder(expiredOn: Deadline,
                                     rates: CurrencyPair => Either[OneFrameServiceError, OneFrameRate])

  case class OneFrameRate(from: String,
                          to: String,
                          bid: BigDecimal,
                          ask: BigDecimal,
                          price: BigDecimal,
                          time_stamp: OffsetDateTime)


}
