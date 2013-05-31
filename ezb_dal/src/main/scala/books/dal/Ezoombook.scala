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

case class Ids(ezbId:UUID, ezlId:UUID, uid:UUID)

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
  def part_id: Option[String]
  def contrib_status: Status.Value
  def contrib_locked: Boolean
  def contrib_content: String
}

case class AtomicContrib(val contrib_id:String,
                         val contrib_type:String,
                         val ezoomlayer_id:UUID,
                         val ezoombook_id: UUID,
                         val user_id: UUID,
                         val part_id: Option[String],
                         val contrib_status: Status.Value,
                         val contrib_locked: Boolean,
                         val contrib_content: String) extends Contrib

object AtomicContrib extends UUIDjsParser{
  import play.api.libs.functional.syntax._

  implicit val contribFmt: Format[AtomicContrib] = (
    (__ \ "contrib_id").format[String] ~
    (__ \ "contrib_type").format[String] ~
    (__ \ "ezoomlayer_id").format[UUID] ~
    (__ \ "ezoombook_id").format[UUID] ~
    (__ \ "user_id").format[UUID] ~
    (__ \ "part_id").formatNullable[String] ~
    (__ \ "contrib_status").format[Status.Value] ~
    (__ \ "contrib_locked").format[Boolean] ~
    (__ \ "contrib_content").format[String]
  )(AtomicContrib.apply, unlift(AtomicContrib.unapply))
}

case class EzlPart(val contrib_id:String,
              val ezoomlayer_id:UUID,
              val ezoombook_id: UUID,
              val user_id: UUID,
              val part_id: Option[String],
              val contrib_status: Status.Value,
              val contrib_locked: Boolean,
              part_title: String,
              part_summary: Option[String],
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
                      ezoomlayer_contribs: List[Contrib])

object EzoomLayer extends UUIDjsParser{
  import play.api.libs.functional.syntax._

  implicit val contribFormat:Format[Contrib] = new Format[Contrib]{
    def reads(json:JsValue):JsResult[Contrib] = (json \ "contrib_type") match {
      case JsString(s) if s == "contrib.Part" => json.validate[EzlPart]
      case _ => json.validate[AtomicContrib]
    }

    def writes(c:Contrib):JsValue = c match{
      case p:EzlPart =>
        Json.toJson(p)(EzlPart.fmt)
      case c:AtomicContrib =>
        Json.toJson(c)(AtomicContrib.contribFmt)
      case _ => throw new RuntimeException("Unknown type for writes[Contrib] " + c)
    }
  }

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

}

object AltFormats extends UUIDjsParser{
  import play.api.libs.functional.syntax._

  def defaultValue[T](v:T)(implicit r:Reads[Option[T]]):Reads[T] = r.map(_.getOrElse(v))

  /**
   * Reads an AtomicContrib from a file withouot ids (like from a contribution in plain text file)
   */
  def atomicContribReads(implicit idz:Ids): Reads[AtomicContrib] = {
    def ctype(implicit r:Reads[String]):Reads[String] = r.map{
      case "contrib.Summary" => "summary"
      case "contrib.Quote" => "quote"
      case _ => "Unknown"
    }

    (
    ((__ \ "contrib_id").read[String] or defaultValue((__ \ "contrib_type").read[String](ctype)+":"+UUID.randomUUID)) ~
        (__ \ "contrib_type").read[String] ~
      ((__ \ "ezoomlayer_id").read[UUID] or defaultValue(idz.ezlId))~
      ((__ \ "ezoombook_id").read[UUID] or defaultValue(idz.ezbId)) ~
      ((__ \ "user_id").read[UUID] or defaultValue(idz.uid))~
        (__ \ "part_id").readNullable[String] ~
      ((__ \ "contrib_status").read[Status.Value] or defaultValue(Status.workInProgress))~
      ((__ \ "contrib_locked").read[Boolean] or defaultValue(false))~
        (__ \ "contrib_content").read[String]
      )(AtomicContrib.apply _)
  }

  def partReads(implicit idz:Ids) = new PartReads(idz)

  implicit def contribReads(implicit idz:Ids):Reads[Contrib] = new Reads[Contrib]{
    def reads(json:JsValue) = (json \ "contrib_type") match {
      case JsString(s) if s == "contrib.Part" => json.validate[EzlPart](partReads)
      case JsString(s)  => json.validate[AtomicContrib](atomicContribReads)
      case x => JsError("Unknown type element " + x)
    }
  }

  class PartReads(idz:Ids) extends Reads[EzlPart] with DefaultReads{
    implicit val ids = idz

    def listReads[A](ra:Reads[A])(implicit bf: collection.generic.CanBuildFrom[List[_], A, List[A]]):Reads[List[A]] = {
      traversableReads[List,A](bf,ra)
    }

    def reads(js:JsValue) = js.validate((
      ((__ \ "contrib_id").read[String] or defaultValue("part:"+UUID.randomUUID)) ~
        ((__ \ "ezoomlayer_id").read[UUID] or defaultValue(idz.ezlId)) ~
        ((__ \ "ezoombook_id").read[UUID] or defaultValue(idz.ezbId))~
        ((__ \ "user_id").read[UUID] or defaultValue(idz.uid)) ~
        (__ \ "part_id").readNullable[String] ~
        ((__ \ "contrib_status").read[Status.Value] or defaultValue(Status.workInProgress))~
        ((__ \ "contrib_locked").read[Boolean] or defaultValue(false)) ~
        (__ \ "part_title").read[String] ~
        (__ \ "part_summary").readNullable[String] ~
        (__ \ "part_contribs").read(listReads[AtomicContrib](atomicContribReads))
      )(EzlPart.apply _))
  }

  def ezlReads(uid:UUID):Reads[EzoomLayer] = new Reads[EzoomLayer]{
    implicit val ezids = Ids(UUID.randomUUID,UUID.randomUUID,uid)

    def reads(js:JsValue) = js.validate((
      ((__ \ "ezoomlayer_id").read[UUID] or defaultValue(ezids.ezlId))  ~
      ((__ \ "ezoombook_id").read[UUID] or defaultValue(ezids.ezbId))  ~
      ((__ \ "ezoomlayer_level").read[Int] or defaultValue(1))  ~
      ((__ \ "ezoomlayer_owner").read[String] or defaultValue("user:"+ezids.uid))  ~
      ((__ \ "ezoomlayer_status").read[Status.Value] or defaultValue(Status.workInProgress))  ~
      ((__ \ "ezoomlayer_locked").read[Boolean] or defaultValue(false))  ~
      (__ \ "ezoomlayer_summaries").read[List[String]] ~
        (__ \ "ezoomlayer_contribs").read[List[Contrib]]
    )(EzoomLayer.apply _))
  }


}