package forex.services.rates

import cats.data.EitherT
import forex.domain.Rate
import errors._
import forex.OneFrameStateDomain.{CurrencyPair, OneFrameRate}

import scala.concurrent.duration.Deadline

trait OneFrameAlgebra[F[_]] {
  def get(pair: Rate.Pair): EitherT[F,OneFrameServiceError,Rate]
}

trait OneFrameCacheProcessorAlgebra[F[_]] {
  def updateWithError(error: OneFrameServiceError):F[Unit]
  def setData(deadline: Deadline, data: List[OneFrameRate]):F[Unit]
  def getCurrencyPair(pair: CurrencyPair): EitherT[F, OneFrameServiceError, OneFrameRate]
}

trait OneFrameHttpRequestHandlerAlgebra[F[_]] {
  def getFreshData:EitherT[F,OneFrameServiceError, String]
}



