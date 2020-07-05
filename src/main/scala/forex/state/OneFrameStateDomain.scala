package forex

import java.time.OffsetDateTime

import cats.effect.Sync
import cats.effect.concurrent.Ref
import forex.services.rates.errors.OneFrameServiceError
import forex.services.rates.errors.OneFrameServiceError.StateInitializationError

import scala.concurrent.duration.Deadline

object OneFrameStateDomain {

  type OneFrameState[F[_]] = Ref[F, Either[OneFrameServiceError,OneFrameRateState]]

  def init[F[_]:Sync]:F[OneFrameState[F]] = Ref.of(Left(StateInitializationError()))

  case class CurrencyPair(from: String, to: String)

  case class OneFrameRateState(expiredOn: Deadline, rates: Map[CurrencyPair, OneFrameRate])

  case class OneFrameRate(from: String,
                          to: String,
                          bid: BigDecimal,
                          ask: BigDecimal,
                          price: BigDecimal,
                          time_stamp: OffsetDateTime)


}
