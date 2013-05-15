package utils

import java.util.UUID

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 05/04/13
 * Time: 11:06
 * To change this template use File | Settings | File Templates.
 */
object Implicits {
  implicit def uuid2Text(id:UUID):String = id.toString()
  implicit def text2UUID(str:String):UUID = UUID.fromString(str)
}
