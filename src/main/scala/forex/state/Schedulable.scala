package forex.state

import fs2.Stream

trait Schedulable[F[_]] {

  /** you can implement it if you need to schedule task inside interpreter **/
  def scheduledTasks:Stream[F,Unit] = Stream.empty

}
