package forex.services.rates


object errors {

  sealed trait OneFrameServiceError {
    def msg:String
    def ex:Option[Throwable]
  }

  trait ApplicationError {
    def ex:Option[Throwable] = None
  }

  object OneFrameServiceError {
    //connection problems
    final case class LookupConnectionError(msg: String, ex:Option[Throwable])
      extends OneFrameServiceError

    // not 2xx response codes
    final case class LookupResponseError(msg: String) extends OneFrameServiceError with ApplicationError

    final case class JsonParsingError(msg: String, ex:Option[Throwable])
      extends OneFrameServiceError

    final case class JsonMappingError(msg: String, ex:Option[Throwable])
      extends OneFrameServiceError

    final case class StateInitializationError (msg:String = "State is not initialized yet")
      extends  OneFrameServiceError with ApplicationError


  }

}
