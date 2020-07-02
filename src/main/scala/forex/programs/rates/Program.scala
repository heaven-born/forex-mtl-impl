package forex.programs.rates

import cats.Functor
import cats.data.EitherT
import errors._
import forex.domain._
import forex.services.RatesService

class Program[F[_]: Functor](
    ratesService: RatesService[F]
) extends Algebra[F] {

  override def get(request: Protocol.GetRatesRequest): F[Error Either Rate] = {
    import request._
    import Rate._
    EitherT(ratesService.get(Pair(from, to)))
      .leftMap(toProgramError)
      .value
  }

}

object Program {

  def apply[F[_]: Functor](
      ratesService: RatesService[F]
  ): Algebra[F] = new Program[F](ratesService)

}
