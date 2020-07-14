package forex.common

object errors {


  trait CommonError {
    def msg:String
    def ex:Throwable
  }

  trait ApplicationError {
    def ex:Throwable = new IllegalStateException("Application error")
  }

}
