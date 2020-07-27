package forex.services.rates.interpreters

import cats.data.{EitherT, Reader}
import cats.effect.Async
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxEitherId}
import com.typesafe.scalalogging.Logger
import forex.OneFrameStateDomain.{CurrencyPair, OneFrameRate, OneFrameRateStateHolder, OneFrameStateRef}
import forex.services.rates.OneFrameCacheProcessorAlgebra
import forex.services.rates.errors.OneFrameServiceError
import forex.services.rates.errors.OneFrameServiceError.NoSuchCurrencyPairError
import forex.utils.{FunctionUtils, TimeUtils}

import scala.concurrent.duration.Deadline


class OneFrameCacheProcessor[F[_]: Async: OneFrameStateRef] extends OneFrameCacheProcessorAlgebra[F] {

  private val logger = Logger[OneFrameCacheProcessor[F]]

  def updateWithError(error: OneFrameServiceError):F[Unit] = {
    logger.error(error.msg)
    OneFrameStateRef[F].update {
      case Left(_) =>
        logger.error(s"Cache was updated with the last error: ${error.msg}")
        Left(error)
      case r @ Right(cache) =>
        if (cache.expiredOn.isOverdue()) {
          logger.error("Cache was expired and removed.")
          Left(error)
        } else r
    }
  }


  def setData(deadline: Deadline, data: List[OneFrameRate]) = {
    val newRates = data.map(d => CurrencyPair(d.from,d.to) -> d).toMap
    val r = Reader(FunctionUtils.toParameterAware(newRates.get)).map{
      case (key,None) => Left(NoSuchCurrencyPairError(s"${key.from}/${key.to}"))
      case (_,Some(r)) => r.asRight[OneFrameServiceError]
    }.run
    logger.info(s"Received new rates with expiration time ${TimeUtils.deadlineToDate(deadline)} for currency paris: ${newRates.keys.mkString(" ")}")
    OneFrameStateRef[F].set(Right(OneFrameRateStateHolder(deadline,r)))
  }

  def getCurrencyPair(pair: CurrencyPair): EitherT[F, OneFrameServiceError, OneFrameRate] = {

    for {
      state <- EitherT(OneFrameStateRef[F].get)
      cacheRate <- EitherT(state.rates(pair).pure[F])
    } yield  cacheRate

  }

}

object OneFrameCacheProcessor {
  implicit def apply[F[_]: Async: OneFrameCacheProcessorAlgebra] =
    implicitly[OneFrameCacheProcessorAlgebra[F]]
}
