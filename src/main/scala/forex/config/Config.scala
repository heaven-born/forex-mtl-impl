package forex.config


import pureconfig.ConfigSource
import pureconfig.generic.auto._

object Config {

  /**
   * @param path the property path inside the default configuration
   */
  //def stream[F[_]: Sync](path: String): Stream[F, ApplicationConfig] = {
   // Stream.eval(Sync[F].delay(
      //ConfigSource.default.at(path).loadOrThrow[ApplicationConfig]))
  //}

  def plain(path: String): ApplicationConfig = {
      ConfigSource.default.at(path).loadOrThrow[ApplicationConfig]
  }

}
