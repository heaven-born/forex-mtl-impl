package forex.programs.rates

import forex.services.rates.errors.OneFrameServiceError

object errors {

  sealed trait RatesProgramError {
    def msg:String
    def ex:Option[Throwable]
  }
  object RatesProgramError {
    final case class RateLookupFailed(msg: String, ex:Option[Throwable] = None ) extends RatesProgramError
  }

  def toProgramError(error: OneFrameServiceError): RatesProgramError = error match {
    case OneFrameServiceError.LookupResponseError(msg) => RatesProgramError.RateLookupFailed(msg, None)
    case OneFrameServiceError.LookupConnectionError(msg,ex) => RatesProgramError.RateLookupFailed(msg,ex)
    case OneFrameServiceError.JsonParsingError(msg,ex) => RatesProgramError.RateLookupFailed(msg,ex)
    case OneFrameServiceError.JsonMappingError(msg,ex) => RatesProgramError.RateLookupFailed(msg,ex)
    case OneFrameServiceError.StateInitializationError(msg) => RatesProgramError.RateLookupFailed(msg, None)
    case  OneFrameServiceError.NoSuchCurrencyPairError(msg) =>RatesProgramError.RateLookupFailed(msg, None)
  }

}
