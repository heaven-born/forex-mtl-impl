package forex.services.rates

import cats.data.{EitherT, Reader}
import cats.effect.Concurrent
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxEitherId}
import com.typesafe.scalalogging.Logger
import forex.OneFrameStateDomain.{CurrencyPair, OneFrameRate, OneFrameRateStateHolder, OneFrameStateRef}
import forex.services.rates.errors.OneFrameServiceError
import forex.services.rates.errors.OneFrameServiceError.NoSuchCurrencyPairError
import forex.utils.{FunctionUtils, TimeUtils}

import scala.concurrent.duration.Deadline

class OneFrameCacheProcessor[F[_]: Concurrent](cache: OneFrameStateRef[F]) {

  private val logger = Logger[OneFrameCacheProcessor[F]]

  private[rates] def updateWithError(error: OneFrameServiceError) = {
    logger.error(error.msg)
    cache.update {
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
    cache.set(Right(OneFrameRateStateHolder(deadline,r)))
  }

  def getCurrencyPair(pair: CurrencyPair): EitherT[F, OneFrameServiceError, OneFrameRate] = {

    for {
      state <- EitherT(cache.get)
      cacheRate <- EitherT(state.rates(pair).pure[F])
    } yield  cacheRate


  }

}
