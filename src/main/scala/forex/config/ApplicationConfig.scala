package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    oneFrameServerHttp: OneFrameServerHttpConfig,
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class OneFrameServerHttpConfig(
     host: String,
     port: Int
)
