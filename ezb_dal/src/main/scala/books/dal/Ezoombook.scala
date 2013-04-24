package books.dal

import books.util.UUIDjsParser

import play.api.libs.json._
import java.util.UUID
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 22/04/13
 * Time: 16:17
 * To change this template use File | Settings | File Templates.
 */

//TODO Values to internationalize
object Status extends Enumeration{
  val workInProgress = Value("Work in progress")
  val published = Value("Published") //Or locked

  implicit val ValWrites:Writes[Value] = new Writes[Value]{
    def writes(v:Value) = JsString(v.toString)
  }

  implicit val ValReads:Reads[Value] = new Reads[Value]{
    def reads(jval:JsValue) = jval match{
      case JsString(s) => JsSuccess(Status.withName(s))
      case _ => JsError("Expected: value, found: " + jval)
    }
  }
}

case class Ezoombook (ezoombook_id:UUID,
                       book_id:UUID,
                       ezoombook_owner:String,
                       ezoombook_status:Status.Value,
                       ezoombook_title:String,
                       ezoombook_public:Boolean,
                       ezoombook_summaries:List[String]) {}

object Ezoombook extends UUIDjsParser{
  implicit val fmt = Json.format[Ezoombook]
}

case class EzoomLayer(ezoomlayer_id: UUID,
                      ezoombook_id: UUID,
                      ezoomlayer_level: Int,
                      ezoomlayer_owner: String,
                      ezoomlayer_status: String,
                      ezoomlayer_locked: Boolean,
                      ezoomlayer_contribs: List[String])

object EzoomLayer extends UUIDjsParser{
//  implicit val ContribWrites:Writes[Contrib] = new Writes[Contrib]{
//    def writes(b:Contrib) = JsString(b.contrib_id)
//  }
//
//  implicit val ContribReads:Reads[Contrib] = new Reads[Contrib]{
//    def reads(jval: JsValue) = jval match{
//      case JsString(s) => JsSuccess(new Contrib(s))
//      case _ => JsError("Expected: id. Found: " + jval)
//    }
//  }
  implicit val fmt = Json.format[EzoomLayer]
}

case class Contrib(contrib_id: String,
                    contrib_type: String,
                    ezoomlayer_id: UUID,
                    ezoombook_id: UUID,
                    user_id: UUID,
                    part_id: String,
                    contrib_status: Status.Value,
                    contrib_locked: Boolean,
                    contrib_content: String){
//  def this (id:String) = {
//    this(id, null, null, null, null, null, null, null, null)
//  }
}

object Contrib extends UUIDjsParser{
  implicit val fmt = Json.format[Contrib]
}