package forex.services.rates.interpreters

import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

import cats.data.EitherT
import cats.effect.{Concurrent, IO, Sync}
import cats.implicits.catsSyntaxEitherId
import forex.OneFrameStateDomain.OneFrameRate
import forex.config.{ApplicationConfig, RatesService}
import forex.services.rates.{OneFrameCacheProcessorAlgebra, OneFrameHttpRequestHandlerAlgebra}
import org.scalamock.scalatest.MockFactory
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import forex.domain.{Currency, Rate, Timestamp}
import forex.services.rates.errors.OneFrameServiceError

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration


class OneFrameServiceTest extends AnyFlatSpec  with Matchers with EitherValues with MockFactory {

  implicit def sync = Sync[IO]
  implicit val cs = IO.contextShift(implicitly[ExecutionContext])
  private implicit val timer = IO.timer(implicitly[ExecutionContext])
  implicit def concurrent = Concurrent[IO]

  implicit val cacheProcessorMock= mock[OneFrameCacheProcessorAlgebra[IO]]
  implicit val handler = mock[OneFrameHttpRequestHandlerAlgebra[IO]]
  implicit val appConfig = ApplicationConfig(null,config)

  val oneFrameLive = new OneFrameService[IO]

  "get" should "accept currency pair and return rate without errors" in {

    val time = OffsetDateTime.parse("2020-07-06T10:45:00.901Z")
    val rate = EitherT(IO(OneFrameRate("AUD","CAD",0,0,0,time).asRight[OneFrameServiceError]))

    cacheProcessorMock.getCurrencyPair _ expects * returns rate

    val inputPari = Rate.Pair(Currency.AUD,Currency.CAD)
    val r = oneFrameLive.get(inputPari).value

    val res = r.unsafeRunSync()

    val expectedRate = Rate(
      pair = inputPari,
      price = forex.domain.Price(0:BigDecimal),
      timestamp = Timestamp(time))

    res.right.value shouldBe expectedRate


  }

  "scheduledTasks" should "should update cache by timer multiple times"  in {

    val numberOfCacheUpdates = 3

    val json = EitherT(IO(validJson.asRight[OneFrameServiceError]))

    handler.getFreshData _  expects() repeat numberOfCacheUpdates returns json

    cacheProcessorMock.setData _  expects(*,*) repeat numberOfCacheUpdates returns IO(())

    oneFrameLive.scheduledTasks.compile.drain.unsafeRunAsyncAndForget()


    Thread.sleep(config.ratesRequestInterval.toSeconds * 1000 * numberOfCacheUpdates)
  }


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



  private lazy val config = RatesService(
    oneFrameServerHttp = null,
    ratesRequestInterval = FiniteDuration(1,TimeUnit.SECONDS),
    ratesRequestRetryInterval = FiniteDuration(1,TimeUnit.SECONDS),
    cacheExpirationTime = FiniteDuration(2,TimeUnit.SECONDS)
  )



}
