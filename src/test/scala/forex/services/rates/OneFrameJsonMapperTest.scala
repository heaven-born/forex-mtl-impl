package forex.services.rates


import java.time.OffsetDateTime

import cats.effect.{IO, Sync}
import forex.OneFrameStateDomain.OneFrameRate
import forex.services.rates.errors.OneFrameServiceError.{JsonMappingError, JsonParsingError}
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global


class OneFrameJsonMapperTest extends AnyFlatSpec  with Matchers with EitherValues {


  implicit def sync = Sync[IO]
  implicit val cs = IO.contextShift(implicitly[ExecutionContext])

  "jsonToRates" should "parse and map valid json" in {

    val r = OneFrameJsonMapper.jsonToRates(validJson).value

    val res = r.unsafeRunSync()

    res.right.value.length shouldBe 1

    res.right.value.head shouldBe OneFrameRate(
        from = "CAD",
        to = "AUD",
        bid = 0.28939350623500215,
        ask = 0.14193151849471397,
        price = 0.21566251236485806,
        time_stamp = OffsetDateTime.parse("2020-07-06T10:45:00.901Z")
    )

  }

  "jsonToRates" should "return parsing error on invalid json processing" in {
    val r = OneFrameJsonMapper.jsonToRates(invalidJson).value
    val res = r.unsafeRunSync()
    res.left.value shouldBe a[JsonParsingError]
   }

  "jsonToRates" should "return mapping error on processing json with missing field" in {
    val r = OneFrameJsonMapper.jsonToRates(missingAskFieldForMapping).value
    val res = r.unsafeRunSync()
    res.left.value shouldBe a[JsonMappingError]
  }

  val invalidJson = "invalid json"

  private val missingAskFieldForMapping =
    """
      |[{
      |   "from":"CAD",
      |   "to":"AUD",
      |   "bid":0.28939350623500215,
      |   "price":0.21566251236485806,
      |   "time_stamp":"2020-07-06T10:45:00.901Z"
      |}]
      |""".stripMargin


  private val validJson =
    """
      |[{
      |   "from":"CAD",
      |   "to":"AUD",
      |   "bid":0.28939350623500215,
      |   "ask":0.14193151849471397,
      |   "price":0.21566251236485806,
      |   "time_stamp":"2020-07-06T10:45:00.901Z"
      |}]
      |""".stripMargin



}
