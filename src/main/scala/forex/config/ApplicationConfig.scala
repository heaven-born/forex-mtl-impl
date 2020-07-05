package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    ratesService: RatesService,
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class RatesService(
    oneFrameServerHttp: OneFrameServerHttpConfig,
    ratesRequestInterval : FiniteDuration,
    ratesRequestRetryInterval : FiniteDuration,
    cacheExpirationTime : FiniteDuration
)

case class OneFrameServerHttpConfig(
    url: String,
    timeout: FiniteDuration
)
