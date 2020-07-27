package forex

import java.time.OffsetDateTime

import cats.effect.Sync
import cats.effect.concurrent.Ref
import forex.services.rates.errors.OneFrameServiceError
import forex.services.rates.errors.OneFrameServiceError.StateInitializationError
import cats.implicits.catsSyntaxEitherId

import scala.concurrent.duration.Deadline

object OneFrameStateDomain {

  object OneFrameStateRef {
    implicit def apply[F[_]: OneFrameStateRef] = implicitly[OneFrameStateRef[F]]
  }

  type OneFrameStateRef[F[_]] = Ref[F, Either[OneFrameServiceError,OneFrameRateStateHolder]]

  def init[F[_]:Sync]:F[OneFrameStateRef[F]] =
    Ref.of(StateInitializationError().asLeft[OneFrameRateStateHolder])

  case class CurrencyPair(from: String, to: String)

  case class OneFrameRateStateHolder(expiredOn: Deadline,
                                     rates: CurrencyPair => Either[OneFrameServiceError,OneFrameRate])

  case class OneFrameRate(from: String,
                          to: String,
                          bid: BigDecimal,
                          ask: BigDecimal,
                          price: BigDecimal,
                          time_stamp: OffsetDateTime)


}
