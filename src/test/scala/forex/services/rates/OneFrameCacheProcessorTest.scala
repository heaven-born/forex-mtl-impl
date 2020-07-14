package forex.services.rates

import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

import cats.effect.{IO, Sync}
import cats.effect.concurrent.Ref
import forex.OneFrameStateDomain.{CurrencyPair, OneFrameRate, OneFrameStateRef}
import forex.services.rates.errors.OneFrameServiceError.{LookupResponseError, NoSuchCurrencyPairError, StateInitializationError}
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.{Deadline, FiniteDuration}
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global


class OneFrameCacheProcessorTest extends AnyFlatSpec  with Matchers with EitherValues {

  implicit def sync = Sync[IO]
  implicit val cs = IO.contextShift(implicitly[ExecutionContext])

  "getCurrencyPair" should "return StateInitializationError on 'clean' cache state" in {
    val r = for {
      state <- Ref.of(Left(StateInitializationError())):IO[OneFrameStateRef[IO]]
      processor = new OneFrameCacheProcessor[IO](state)
      noSuchPariError <- processor.getCurrencyPair(CurrencyPair("any","any")).value
    } yield noSuchPariError

    val res = r.unsafeRunSync()

    res.left.value shouldBe a[StateInitializationError]

  }

  it should "return NoSuchCurrencyPairError if currency pari is missing in cache" in {
    val r = for {
      state <- Ref.of(Left(StateInitializationError())):IO[OneFrameStateRef[IO]]
      processor = new OneFrameCacheProcessor[IO](state)
      rate = List(OneFrameRate("","",0,0,0,OffsetDateTime.now()))
      _ <-  processor.setData(Deadline.now, rate)
      noSuchPariError <- processor.getCurrencyPair(CurrencyPair("any","any")).value
    } yield noSuchPariError

    val res = r.unsafeRunSync()

    res.left.value shouldBe a[NoSuchCurrencyPairError]

  }

  it should "return pair when such pair is available in cache" in {
    val rate = OneFrameRate("EUR","USD",0,0,0,OffsetDateTime.now())
    val r = for {
      state <- Ref.of(Left(StateInitializationError())):IO[OneFrameStateRef[IO]]
      processor = new OneFrameCacheProcessor[IO](state)
      _ <-  processor.setData(Deadline.now, List(rate))
      noSuchPariError <- processor.getCurrencyPair(CurrencyPair("EUR","USD")).value
    } yield noSuchPariError

    val res = r.unsafeRunSync()

    res.right.value shouldBe rate

  }

  "updateWithError" should "replace prev error StateInitializationError with new error NoSuchCurrencyPairError" in {
    val r = for {
      state <- Ref.of(Left(StateInitializationError())):IO[OneFrameStateRef[IO]]
      processor = new OneFrameCacheProcessor[IO](state)
      _ <-  processor.updateWithError(NoSuchCurrencyPairError("new error"))
      noSuchPariError <- processor.getCurrencyPair(CurrencyPair("EUR","USD")).value
    } yield noSuchPariError

    val res = r.unsafeRunSync()

    res.left.value shouldBe a[NoSuchCurrencyPairError]

  }

  it should "replace valid state with error if state is expired" in {
    val rate = OneFrameRate("EUR","USD",0,0,0,OffsetDateTime.now())
    val r = for {
      state <- Ref.of(Left(StateInitializationError())):IO[OneFrameStateRef[IO]]
      processor = new OneFrameCacheProcessor[IO](state)
      _ <-  processor.setData(Deadline.now, List(rate))
      _ <-  processor.updateWithError(LookupResponseError("new error"))
      noSuchPariError <- processor.getCurrencyPair(CurrencyPair("EUR","USD")).value
    } yield noSuchPariError

    val res = r.unsafeRunSync()

    res.left.value shouldBe a[LookupResponseError]

  }

  it should "keep valid state if state is NOT expired" in {
    val rate = OneFrameRate("EUR","USD",0,0,0,OffsetDateTime.now())
    val notExpiredDeadline = FiniteDuration(365,TimeUnit.DAYS).fromNow

    val r = for {
      state <- Ref.of(Left(StateInitializationError())):IO[OneFrameStateRef[IO]]
      processor = new OneFrameCacheProcessor[IO](state)
      _ <-  processor.setData(notExpiredDeadline, List(rate))
      _ <-  processor.updateWithError(LookupResponseError("new error"))
      noSuchPariError <- processor.getCurrencyPair(CurrencyPair("EUR","USD")).value
    } yield noSuchPariError

    val res = r.unsafeRunSync()

    res.right.value shouldBe rate

  }



}
