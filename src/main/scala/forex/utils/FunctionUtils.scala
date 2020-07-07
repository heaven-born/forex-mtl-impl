package forex.utils

object FunctionUtils {

  def toParameterAware[K,V](f:K=>V) = (p:K) => (p,f(p))


}
