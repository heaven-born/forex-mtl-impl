package forex.http
package rates

import cats.effect.Sync
import cats.implicits.catsSyntaxTuple2Semigroupal
import cats.syntax.flatMap._
import forex.programs.RatesProgram
import forex.programs.rates.{Protocol => RatesProgramProtocol}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._, QueryParams._, Protocol._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(from) +& ToQueryParam(to) =>
      (from,to).mapN { (from, to) =>
        rates
          .get(RatesProgramProtocol.GetRatesRequest(from, to))
          .flatMap {
            case Left(error) =>
              error.ex.foreach(_.printStackTrace())
              InternalServerError(s"${error.msg}\r\n")
            case Right(result) =>
              Ok(result.asGetApiResponse)
          }
      }.fold(
        err => BadRequest(s"Could not parse arguments 'from' or 'to' Reason: ${err.head.details}"),
        fa => fa
      )
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
