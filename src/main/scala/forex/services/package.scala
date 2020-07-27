package forex

import forex.state.Schedulable

package object services {
  type RatesService[F[_]] = rates.OneFrameAlgebra[F] with Schedulable[F]
}
