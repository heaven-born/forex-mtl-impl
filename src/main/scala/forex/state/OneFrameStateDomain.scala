package forex

import java.time.OffsetDateTime

import cats.effect.Sync
import cats.effect.concurrent.Ref

object OneFrameStateDomain {

  def init[F[_]:Sync] = Ref.of(OneFrameRateState(0,Map()))

  case class CurrencyPair(from: String, to: String)

  case class OneFrameRateState(timestamp: Long, rates: Map[CurrencyPair, OneFrameRate])

  case class OneFrameRate(from: String,
                          to: String,
                          bid: BigDecimal,
                          ask: BigDecimal,
                          price: BigDecimal,
                          time_stamp: OffsetDateTime)


}
