package util

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 10/10/13
 * Time: 14:05
 * To change this template use File | Settings | File Templates.
 */
object EzbImplicits {
  implicit def any2option[A](value:A):Option[A] = {
    if (value == null)
      None
    else
      Some(value)
  }
}
