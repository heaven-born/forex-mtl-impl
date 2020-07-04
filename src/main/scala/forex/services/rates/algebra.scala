package forex.services.rates

import forex.domain.Rate
import fs2.Stream
import errors._

trait Algebra[F[_]] {
  def get(pair: Rate.Pair): F[RatesServiceError Either Rate]

  /** you can implement it if you need to schedule task inside interpreter **/
  def scheduledTasks:Stream[F,Unit] = Stream.empty
}
