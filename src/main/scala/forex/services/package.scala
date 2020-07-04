package forex

import forex.state.Schedulable

package object services {
  type RatesService[F[_]] = rates.Algebra[F] with Schedulable[F]
  final val RatesServices = rates.Interpreters


}
