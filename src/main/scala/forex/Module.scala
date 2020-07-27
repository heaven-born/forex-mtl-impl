package forex

import cats.effect.{Concurrent, Timer}
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.programs.rates.RatesProgram
import forex.services.RatesService
import forex.services.rates.LiveInstances
import forex.state.SharedState
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.middleware.{AutoSlash, Timeout}

class Module[F[_]: Concurrent: Timer](config: ApplicationConfig, sharedState: SharedState[F]) {

  val live = new LiveInstances[F](config,sharedState.oneFrame)
  import live._

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](RatesProgram[F]).routes

  //it can merge streams from multiple services
  def scheduledTasks = implicitly[RatesService[F]].scheduledTasks

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware =
    (http: HttpRoutes[F]) => AutoSlash(http)

  private val appMiddleware: TotalMiddleware =
    (http: HttpApp[F]) => Timeout(config.http.timeout)(http)

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

}
