package forex

import cats.{Applicative, FlatMap}
import cats.effect._
import cats.implicits.{catsSyntaxApplicativeId, toFlatMapOps}
import cats.syntax.functor._
import forex.config.{ApplicationConfig, _}
import forex.services.state.oneFrameSharedState
import fs2.Stream
import org.http4s.server.blaze.BlazeServerBuilder

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    for {
        app <- new Application[IO].build
        res <- app.compile.drain.as(ExitCode.Success)
    } yield  res


}

class Application[F[_]: ConcurrentEffect: Timer: Applicative: FlatMap] {

  def build: F[Stream[F, Unit]] = {

    val config = Config.plain("app")
    val module = new Module[F](config)

    for {
      state <- oneFrameSharedState
      server <- buildServer(config,module).pure[F]
     } yield server.merge(module.scheduledTasks)

  }

  private def buildServer(config: ApplicationConfig, module: Module[F]) = {
    for {
      _ <- BlazeServerBuilder[F]
        .bindHttp(config.http.port, config.http.host)
        .withHttpApp(module.httpApp)
        .serve
    } yield ()
  }

}
