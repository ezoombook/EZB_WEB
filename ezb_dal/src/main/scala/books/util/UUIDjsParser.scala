package books.util

import play.api.libs.json._
import java.util.UUID
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 23/04/13
 * Time: 17:42
 * To change this template use File | Settings | File Templates.
 */
trait UUIDjsParser {
  implicit val UUIDWrites:Writes[UUID] = new Writes[UUID] {
    def writes(o:UUID) = JsString(o.toString)
  }

  implicit val UUIDReads:Reads[UUID] = new Reads[UUID] {
    def reads(jval: JsValue) = jval match{
      case JsString(s) => JsSuccess(UUID.fromString(s))
      case _ => JsError("Expected: UUID. Found: " + jval)
    }
  }

}
