package forex

import cats.{Applicative, FlatMap}
import cats.effect._
import cats.implicits.toFlatMapOps
import cats.syntax.functor._
import forex.config._
import forex.state.SharedState
import org.http4s.server.blaze.BlazeServerBuilder
import cats.effect.ExitCode

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
        new Application[IO].build


}

class Application[F[_]: ConcurrentEffect: Timer: Applicative: FlatMap] {

  def build:F[ExitCode]  = {

    val config = Config.plain("app")

    for {
      oneFrameState <- OneFrameStateDomain.init
      sharedState = SharedState(oneFrameState)
      module = new Module[F](config, sharedState)
      exitCode <- BlazeServerBuilder[F]
        .bindHttp(config.http.port, config.http.host)
        .withHttpApp(module.httpApp)
        .serve
        .merge(module.scheduledTasks)
        .compile.lastOrError
     } yield exitCode.asInstanceOf[ExitCode]

  }

}
