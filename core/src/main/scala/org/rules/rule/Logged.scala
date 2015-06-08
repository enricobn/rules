package org.rules.rule

/**
 * Created by enrico on 6/7/15.
 */
object Logged {

  implicit def stringToLoggedError[T](errorMessage: String) : Logged[T] = {
    Logged(None, List(Error(errorMessage)))
  }

  implicit def messagesToLogged[T](messages: List[Message]) : Logged[T] = {
    Logged(None, messages)
  }

  implicit def valueToLogged[T](value: T) : Logged[T] = {
    Logged(Some(value))
  }

}

case class Logged[T](value: Option[T], messages: List[Message] = List.empty) {

  /**
   * if this is not valid then this otherwise a Logged with no messages, otherwise applies the function to values
   * TODO warnings
   * @param v
   * @param func
   * @return
   */
  def fold(v: T) (func: (T, T) => T) : Logged[T] = {
    value match {
      case Some(good) => func(v, good)
      case _ => this
    }
  }

  /**
   * if logged and this are valid applies function to values, if both are invalid a sum of messages, otherwise
   * an invalid value with messages
   * TODO warnings
   * @param logged
   * @param func
   * @return
   */
  def fold(logged: Logged[T]) (func: (T, T) => T) : Logged[T] = {
    value match {
      case Some(v) =>
        logged.value match {
          case Some(l) => func(v,l)
          case _ => Logged(None, logged.messages)
        }
      case _ =>
        logged.value match {
          case Some(l) => Logged(None, messages)
          case _ => Logged(None, messages ++ logged.messages)
        }
    }
  }

}
