package forex.services.rates

import cats.implicits.toShow
import forex.OneFrameStateDomain.{CurrencyPair, OneFrameRate}
import forex.domain.{Currency, Price, Rate, Timestamp}

object CacheDomainConverter {

  private[rates] def convert(rate: OneFrameRate) =  {
    val fromCurrency = Currency.fromString(rate.from)
    val toCurrency = Currency.fromString(rate.to)
    val price = rate.price
    val timestamp = rate.time_stamp
    Rate(Rate.Pair(fromCurrency,toCurrency), Price(price), Timestamp(timestamp))
  }

  private[rates] def convert[rates](pair: Rate.Pair) =
    CurrencyPair(from = pair.from.show, to = pair.to.show)



}
