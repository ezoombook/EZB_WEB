package utils

import books.dal.Status
import play.api.data.format.Formatter
import play.api.data.FormError
import java.util.UUID

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 12/06/13
 * Time: 20:14
 * To change this template use File | Settings | File Templates.
 */
trait FormHelpers {
  implicit def str2Status(strVal:String):Status.Value = Status.withName(strVal)

  implicit def statusFormat: Formatter[Status.Value] = new Formatter[Status.Value]{
    def bind(key: String, data: Map[String, String]) =
      data.get(key).map(Status.withName(_)).toRight(Seq(FormError(key, "error.required", Nil)))

    def unbind(key: String, value: Status.Value): Map[String, String] = Map(key -> value.toString)
  }

  implicit def uuidForam:Formatter[UUID] = new Formatter[UUID]{
    def bind(key:String, data:Map[String,String]) =
      data.get(key).map(UUID.fromString(_)).toRight(Seq(FormError(key, "error.required", Nil)))

    def unbind(key:String, value: UUID):Map[String, String] = Map(key -> value.toString)
  }
}
