package forex.services.rates

object errors {

  sealed trait RatesServiceError {
    def msg:String
    def ex:Option[Throwable]
  }
  object RatesServiceError {
    //connection problems
    final case class OneFrameLookupConnectionError(msg: String, ex:Option[Throwable] = None)
      extends RatesServiceError

    // not 2xx response codes
    final case class OneFrameLookupResponseError(msg: String, ex:Option[Throwable] = None)
      extends RatesServiceError

    final case class OneFrameLookupJsonParsingError(msg: String, ex:Option[Throwable] = None)
      extends RatesServiceError

    final case class OneFrameLookupJsonMappingError(msg: String, ex:Option[Throwable] = None)
      extends RatesServiceError

  }

}
