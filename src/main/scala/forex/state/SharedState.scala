package forex.state

import cats.effect.Sync
import cats.effect.concurrent.Ref
import forex.OneFrameStateDomain.OneFrameRateState

/** this class aggregates all shared states used in the application. Add new val if needed. **/
case class SharedState[F[_]:Sync] (oneFrame: Ref[F, Option[OneFrameRateState]])





