package forex.programs.rates

import forex.services.rates.errors.RatesServiceError

object errors {

  sealed trait RatesProgramError {
    def msg:String
    def ex:Option[Throwable]
  }
  object RatesProgramError {
    final case class RateLookupFailed(msg: String, ex:Option[Throwable] = None ) extends RatesProgramError
  }

  def toProgramError(error: RatesServiceError): RatesProgramError = error match {
    case RatesServiceError.OneFrameLookupResponseError(msg,ex) => RatesProgramError.RateLookupFailed(msg,ex)
    case RatesServiceError.OneFrameLookupConnectionError(msg,ex) => RatesProgramError.RateLookupFailed(msg,ex)
    case RatesServiceError.OneFrameLookupJsonParsingError(msg,ex) => RatesProgramError.RateLookupFailed(msg,ex)
    case RatesServiceError.OneFrameLookupJsonMappingError(msg,ex) => RatesProgramError.RateLookupFailed(msg,ex)
  }

}
