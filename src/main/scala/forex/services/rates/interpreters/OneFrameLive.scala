package forex.services.rates.interpreters

import cats.Applicative
import forex.config.OneFrameServerHttpConfig
import forex.domain.Rate
import forex.services.rates.Algebra
import forex.services.rates.errors._

class OneFrameLive[F[_]: Applicative](config: OneFrameServerHttpConfig) extends Algebra[F] {


  override def get(pair: Rate.Pair): F[Error Either Rate] = ???

  //allSupportedCurrencies.combinations(2) .flatMap(_.permutations) .map(p=>"pair="+p(0)+p(1)+"&")

}
