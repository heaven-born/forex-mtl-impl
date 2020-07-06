package forex.services.rates

import cats.data.EitherT
import cats.effect.Concurrent
import cats.implicits.catsSyntaxApplicativeId
import com.typesafe.scalalogging.Logger
import forex.OneFrameStateDomain.{CurrencyPair, OneFrameRate, OneFrameRateState, OneFrameState}
import forex.services.rates.errors.OneFrameServiceError
import forex.services.rates.errors.OneFrameServiceError.NoSuchCurrencyPairError
import forex.utils.TimeUtils

import scala.concurrent.duration.Deadline

case class OneFrameCacheProcessor[F[_]: Concurrent](cache: OneFrameState[F]) {

  private val logger = Logger[OneFrameCacheProcessor[F]]

  def updateWithError(error: OneFrameServiceError) = {
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
    cache.set(Right(OneFrameRateState(deadline,newRates)))
  }

  def getCurrencyPair(pair: CurrencyPair) = {

    def currencyPairValidation(state: OneFrameRateState) =
      if (state.rates.contains(pair)) Right(state)
      else Left(NoSuchCurrencyPairError(s"${pair.from}/${pair.to}"):OneFrameServiceError)

    for {
      state <- EitherT(cache.get)
      _     <- EitherT(currencyPairValidation(state).pure[F])
      cacheRate = state.rates(pair)
    } yield  cacheRate


  }

}
