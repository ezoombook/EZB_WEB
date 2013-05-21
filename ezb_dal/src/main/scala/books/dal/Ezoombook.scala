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
                       ezoombook_public:Boolean
                       )

object Ezoombook extends UUIDjsParser{
  implicit val fmt = Json.format[Ezoombook]
}

trait Contrib{
  def contrib_id: String
  def contrib_type: String
  def ezoomlayer_id: UUID
  def ezoombook_id: UUID
  def user_id: UUID
  def part_id: String
  def contrib_status: Status.Value
  def contrib_locked: Boolean
  def contrib_content: String
}

//class Contrib(contrib_id: String,
//                   contrib_type: String,
//                   ezoomlayer_id: UUID,
//                   ezoombook_id: UUID,
//                   user_id: UUID,
//                   part_id: String,
//                   contrib_status: Status.Value,
//                   contrib_locked: Boolean,
//                   contrib_content: String)
//object Contrib extends UUIDjsParser{
//  implicit val contribFmt = Json.format[Contrib]
//}

case class AtomicContrib(val contrib_id:String,
                         val contrib_type:String,
                         val ezoomlayer_id:UUID,
                         val ezoombook_id: UUID,
                         val user_id: UUID,
                         val part_id: String,
                         val contrib_status: Status.Value,
                         val contrib_locked: Boolean,
                         val contrib_content: String) extends Contrib

object AtomicContrib extends UUIDjsParser{
  import play.api.libs.functional.syntax._

//  def contribTypeReads(implicit r:Reads[String]):Reads[String] =
//    r.map{
//      case "contrib.Summary" => "quote:" +UUID.randomUUID.toString
//      case "contrib.Quote" => "summary:" +UUID.randomUUID.toString
//      case _ => throw new RuntimeException("Unrecognized type of contrib!")
//    }
//
//  val typeFormat:Format[String] = Format(contribTypeReads, Writes.StringWrites)

  implicit val contribFmt: Format[AtomicContrib] = (
    (__ \ "contrib_id").format[String] ~
    (__ \ "contrib_type").format[String] ~
    (__ \ "ezoomlayer_id").format[UUID] ~
    (__ \ "ezoombook_id").format[UUID] ~
    (__ \ "user_id").format[UUID] ~
    (__ \ "part_id").format[String] ~
    (__ \ "contrib_status").format[Status.Value] ~
    (__ \ "contrib_locked").format[Boolean] ~
    (__ \ "contrib_content").format[String]
  )(AtomicContrib.apply, unlift(AtomicContrib.unapply))
}

case class EzlPart(val contrib_id:String,
              val ezoomlayer_id:UUID,
              val ezoombook_id: UUID,
              val user_id: UUID,
              val part_id: String,
              val contrib_status: Status.Value,
              val contrib_locked: Boolean,
              part_title: String,
              part_summary: String,
              part_contribs: List[AtomicContrib]) extends Contrib{
  val contrib_type = "contrib.Part"
  val contrib_content = ""
}

object EzlPart extends UUIDjsParser{
  implicit val fmt = Json.format[EzlPart]
}

case class EzoomLayer(ezoomlayer_id: UUID,
                      ezoombook_id: UUID,
                      ezoomlayer_level: Int,
                      ezoomlayer_owner: String,
                      ezoomlayer_status: Status.Value,
                      ezoomlayer_locked: Boolean,
                      ezoomlayer_summaries:List[String],
                      ezoomlayer_parts: List[Contrib])

object EzoomLayer extends UUIDjsParser{
  import play.api.libs.functional.syntax._

  implicit val fmt: Format[EzoomLayer] = (
    (__ \ "ezoomlayer_id").format[UUID] ~
        (__ \ "ezoombook_id").format[UUID] ~
        (__ \ "ezoomlayer_level").format[Int] ~
        (__ \ "ezoomlayer_owner").format[String] ~
        (__ \ "ezoomlayer_status").format[Status.Value] ~
        (__ \ "ezoomlayer_locked").format[Boolean] ~
        (__ \ "ezoomlayer_summaries").format[List[String]] ~
        (__ \ "ezoomlayer_parts").format[List[Contrib]]
  )(EzoomLayer.apply, unlift(EzoomLayer.unapply))

//  implicit val fmt = new Format[EzoomLayer]{ //Json.format[EzoomLayer]
//    def reads(json:JsValue):JsResult[EzoomLayer] =
//    (__ \ "ezoomlayer_id").read[UUID] ~
//      (__ \ "ezoombook_id").read[UUID] ~
//      (__ \ "ezoomlayer_level").read[Int] ~
//      (__ \ "ezoomlayer_owner").read[String] ~
//      (__ \ "ezoomlayer_status").read[Status.Value] ~
//      (__ \ "ezoomlayer_locked").read[Boolean] ~
//      (__ \ "ezoomlayer_summaries").read(list[String]) ~
//      (__ \ "ezoomlayer_parts").read(list[Contrib])
//    )(EzoomLayer)
//
//    def writes
//  }

  implicit val contribFormat:Format[Contrib] = new Format[Contrib]{
    def reads(json:JsValue):JsResult[Contrib] = (json \ "contrib_type") match {
      case JsString(s) if s == "contrib.Part" => JsSuccess(json.as[EzlPart])
      case _ => JsSuccess(json.as[AtomicContrib])
    }

    def writes(c:Contrib):JsValue = c match{
      case p:EzlPart => println("[DBG] - writing part... ")
        Json.toJson(p)
      case c:AtomicContrib => println("[DBG] - writing atomic contrib")
        Json.toJson(c)
      case _ => throw new RuntimeException("Unknown type for writes[Contrib]")
    }
  }
}

