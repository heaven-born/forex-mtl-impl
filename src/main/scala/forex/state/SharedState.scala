package forex.state

import cats.effect.Sync
import forex.OneFrameStateDomain.OneFrameState

/** this class aggregates all shared states used in the application. Add new val if needed. **/
case class SharedState[F[_]:Sync] (oneFrame: OneFrameState[F])





