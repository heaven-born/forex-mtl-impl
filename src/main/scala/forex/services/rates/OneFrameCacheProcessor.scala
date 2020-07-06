package forex.services.rates

import cats.data.EitherT
import cats.effect.Concurrent
import cats.implicits.catsSyntaxApplicativeId
import com.typesafe.scalalogging.Logger
import forex.OneFrameStateDomain.{CurrencyPair, OneFrameRate, OneFrameRateStateHolder, OneFrameStateRef}
import forex.services.rates.errors.OneFrameServiceError
import forex.services.rates.errors.OneFrameServiceError.NoSuchCurrencyPairError
import forex.utils.TimeUtils

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
    logger.info(s"Received new rates with expiration time ${TimeUtils.deadlineToDate(deadline)} for currency paris: ${newRates.keys.mkString(" ")}")
    cache.set(Right(OneFrameRateStateHolder(deadline,newRates)))
  }

  def getCurrencyPair(pair: CurrencyPair): EitherT[F, OneFrameServiceError, OneFrameRate] = {

    def currencyPairValidation(state: OneFrameRateStateHolder) =
      if (state.rates.contains(pair)) Right(state)
      else Left(NoSuchCurrencyPairError(s"${pair.from}/${pair.to}"):OneFrameServiceError)

    for {
      state <- EitherT(cache.get)
      _     <- EitherT(currencyPairValidation(state).pure[F])
      cacheRate = state.rates(pair)
    } yield  cacheRate


  }

}
