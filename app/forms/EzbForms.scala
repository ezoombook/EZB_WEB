package forms

import models._
import users.dal._
import books.dal._

import play.api.data._
import format.Formats._
import play.api.data.format.Formatter
import play.api.cache
import cache.Cache
import Forms._
import java.util.UUID
import users.dal.User
import books.dal.BookPart
import play.api.data.FormError
import scala.Some
import play.api.libs.json.JsValue

object EzbForms {

  val bookForm = Form[Book](
    mapping(
      "book_id" -> of[UUID],
      "title" -> text,
      "authors" -> list(text),
      "languages" -> list(text),
      "publishers" -> list(text),
      "published_dates" -> list(text),
      "tags" -> list(text),
      "summary" -> text
    )((id,title,authors,languages,publishers,published_dates,tags,summary) =>
      Book(id,title,authors,languages,publishers,published_dates, tags, summary, List[BookPart]()))
      ((book:Book) => Some(book.bookId, book.bookTitle, book.bookAuthors, book.bookLanguages, book.bookPublishers, book.bookPublishedDates,
        book.bookTags, book.bookSummary))
  )

  val ezoomBookForm = Form[Ezoombook](
    mapping(
      "ezb_id" -> of[UUID],
      "book_id" -> of[UUID],
      "ezb_owner" -> text,
      "ezb_status" -> default[Status.Value](of[Status.Value], Status.workInProgress),
      "ezb_title" -> text,
      "ezb_public" -> default(boolean, false)
    )(Ezoombook.apply)(Ezoombook.unapply)
  )

  def ezoomlayerForm(ezoombookid:UUID, layerid:UUID, userid:UUID) = Form[EzoomLayer](
    mapping(
      "ezoomlayer_id" -> default(of[UUID], layerid),
      "ezoombook_id" -> default(of[UUID], ezoombookid),
      "ezoomlayer_level" -> default(number, 1),
      "ezoomlayer_owner" -> default(text, "user:"+userid),
      "ezoomlayer_status" -> default[Status.Value](of[Status.Value], Status.workInProgress),
      "ezoomlayer_locked" -> default(boolean, false),
      "ezoomlayer_summaries" -> list(text),
      "ezoomlayer_contribs" -> list(contribMapping("",layerid,ezoombookid,userid))
    )(EzoomLayer.apply)(EzoomLayer.unapply)
  )

  def ezoomlayerForm = Form[EzoomLayer](
    mapping(
      "ezoomlayer_id" -> of[UUID],
      "ezoombook_id" -> of[UUID],
      "ezoomlayer_level" -> number,
      "ezoomlayer_owner" -> text,
      "ezoomlayer_status" -> of[Status.Value],
      "ezoomlayer_locked" -> boolean,
      "ezoomlayer_summaries" -> list(text),
      "ezoomlayer_contribs" -> list(contribMapping)
    )(EzoomLayer.apply)
      (EzoomLayer.unapply)
  )

  private def contribMapping(partId:String,ezlId:UUID,ezbId:UUID,uid:UUID):Mapping[Contrib] = mapping(
    "contrib_id" -> default(text, UUID.randomUUID().toString),
    "contrib_type" -> nonEmptyText,
    "ezoomlayer_id" -> default(of[UUID], ezlId),
    "ezoombook_id" -> default(of[UUID], ezbId),
    "user_id" -> default(of[UUID], uid),
    "contrib_part" -> optional(text),
    "contrib_status" -> default[Status.Value](of[Status.Value], Status.workInProgress),
    "contrib_locked" -> default(boolean, false),
    "contrib_content" -> default(text, ""),
    "part_title" -> optional(text),
    "part_summary" -> optional(text),
    "part_contribs" -> optional(list(atomicContribMapping(ezlId,ezbId,uid,partId)))
  )(contribApply)(contribUnapply)

  private def contribMapping:Mapping[Contrib] = mapping(
    "contrib_id" -> text,
    "contrib_type" -> text,
    "ezoomlayer_id" -> of[UUID],
    "ezoombook_id" -> of[UUID],
    "user_id" -> of[UUID],
    "contrib_part" -> optional(text),
    "contrib_status" -> of[Status.Value],
    "contrib_locked" -> boolean,
    "contrib_content" -> text,
    "part_title" -> optional(text),
    "part_summary" -> optional(text),
    "part_contribs" -> optional(list(atomicContribMapping))
  )(contribApply)(contribUnapply)

  private def atomicContribMapping:Mapping[AtomicContrib] = mapping(
    "contrib_id" -> text,
    "contrib_type" -> text,
    "ezoomlayer_id" -> of[UUID],
    "ezoombook_id" -> of[UUID],
    "user_id" -> of[UUID],
    "contrib_part" -> optional(text),
    "contrib_status" -> of[Status.Value],
    "contrib_locked" -> boolean,
    "contrib_content" -> text
  )(AtomicContrib.apply)(AtomicContrib.unapply)

  private def atomicContribMapping(ezlid:UUID,ezbid:UUID,uid:UUID,partid:String):Mapping[AtomicContrib] = mapping(
    "contrib_id" -> ignored(UUID.randomUUID().toString),
    "contrib_type" -> text,
    "ezoomlayer_id" -> ignored(ezlid),
    "ezoombook_id" -> ignored(ezbid),
    "user_id" -> ignored(uid),
    "contrib_part" -> optional(ignored(partid)),
    "contrib_status" -> default[Status.Value](of[Status.Value], Status.workInProgress),
    "contrib_locked" -> default(boolean, true),
    "contrib_content" -> text
  )(AtomicContrib.apply)(AtomicContrib.unapply)

  private def contribApply(
    contrib_id: String,
    contrib_type: String,
    ezoomlayer_id: UUID,
    ezoombook_id: UUID,
    user_id: UUID,
    part_id: Option[String],
    contrib_status: Status.Value,
    contrib_locked: Boolean,
    contrib_content: String,
    part_title: Option[String],
    part_summary: Option[String],
    part_contribs: Option[List[AtomicContrib]]):Contrib = contrib_type match{
      case "contrib.Part" =>
        EzlPart(contrib_id,ezoomlayer_id,ezoombook_id,user_id,part_id,contrib_status,contrib_locked,
          part_title.getOrElse(""),part_summary,part_contribs.getOrElse(List[AtomicContrib]()))
      case _ =>
        AtomicContrib(contrib_id,contrib_type,ezoomlayer_id,ezoombook_id,
          user_id,part_id,contrib_status,contrib_locked,contrib_content)
  }

  private def contribUnapply(contrib:Contrib) = Some(
    contrib.contrib_id,
    contrib.contrib_type,
    contrib.ezoomlayer_id,
    contrib.ezoombook_id,
    contrib.user_id,
    contrib.part_id,
    contrib.contrib_status,
    contrib.contrib_locked,
    contrib.contrib_content,
    contrib match{
      case part:EzlPart => Some(part.part_title)
      case _ => None
    },
    contrib match{
      case part:EzlPart => part.part_summary
      case _ => None
    },
    contrib match{
      case part:EzlPart => Some(part.part_contribs)
      case _ => None
    }
  )

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
