package forex.programs.rates

import cats.Functor
import cats.data.EitherT
import errors._
import forex.domain._
import forex.services.RatesService

class RatesProgram[F[_]: Functor: RatesService] extends RatesProgramAlgebra[F] {

  override def get(request: Protocol.GetRatesRequest): F[RatesProgramError Either Rate] = {
    import request._
    import Rate._
    EitherT(implicitly[RatesService[F]].get(Pair(from, to)))
      .leftMap(toProgramError)
      .value
  }

}

object RatesProgram {

  def apply[F[_]: Functor:RatesService]: RatesProgramAlgebra[F] = new RatesProgram[F]

}
