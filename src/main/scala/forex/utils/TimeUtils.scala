package forex.utils

import java.time.OffsetDateTime

import scala.concurrent.duration.Deadline

object TimeUtils {

  def deadlineToDate (d: Deadline) =  OffsetDateTime.now().plusSeconds(d.time.toSeconds)

}
