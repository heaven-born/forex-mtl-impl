package forex.programs.rates

import forex.common.errors.{ApplicationError, CommonError}
import forex.services.rates.errors.OneFrameServiceError

object errors {

  sealed trait RatesProgramError extends CommonError

  object RatesProgramError {
    final case class RateLookupFailedSystemError(msg: String, ex:Throwable)
      extends RatesProgramError

    final case class RateLookupFailedAppError(msg: String)
      extends RatesProgramError with ApplicationError
  }

  def toProgramError(error: OneFrameServiceError): RatesProgramError = error match {
    case OneFrameServiceError.LookupResponseError(msg) => RatesProgramError.RateLookupFailedAppError(msg)
    case OneFrameServiceError.LookupConnectionError(msg,ex) => RatesProgramError.RateLookupFailedSystemError(msg,ex)
    case OneFrameServiceError.JsonParsingError(msg,ex) => RatesProgramError.RateLookupFailedSystemError(msg,ex)
    case OneFrameServiceError.JsonMappingError(msg,ex) => RatesProgramError.RateLookupFailedSystemError(msg,ex)
    case OneFrameServiceError.StateInitializationError(msg) => RatesProgramError.RateLookupFailedAppError(msg)
    case OneFrameServiceError.NoSuchCurrencyPairError(msg) =>RatesProgramError.RateLookupFailedAppError(msg)
  }

}
