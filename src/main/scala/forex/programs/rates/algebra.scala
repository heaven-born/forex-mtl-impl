package forex.programs.rates

import forex.domain.Rate
import errors._

trait RatesProgramAlgebra[F[_]] {
  def get(request: Protocol.GetRatesRequest): F[RatesProgramError Either Rate]
}
