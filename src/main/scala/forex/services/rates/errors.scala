package forex.services.rates

import forex.common.errors.{ApplicationError, CommonError}


object errors {

  sealed trait OneFrameServiceError extends CommonError

  object OneFrameServiceError {
    //connection problems
    final case class LookupConnectionError(msg: String, ex:Throwable)
      extends OneFrameServiceError

    // not 2xx response codes
    final case class LookupResponseError(msg: String) extends OneFrameServiceError with ApplicationError

    final case class JsonParsingError(msg: String, ex:Throwable)
      extends OneFrameServiceError

    final case class JsonMappingError(msg: String, ex:Throwable)
      extends OneFrameServiceError

    final case class StateInitializationError (msg:String = "State is not initialized yet")
      extends  OneFrameServiceError with ApplicationError

    final case class NoSuchCurrencyPairError(pair:String)
      extends  OneFrameServiceError with ApplicationError {
       def msg = s"No such currency pair available in cache $pair"
    }


  }

}
