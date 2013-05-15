package controllers

import models._
import users.dal._
import books.dal._
import utils.Implicits._

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

  val userForm = Form(
    mapping(
      "username" -> text,
      "mail" -> email,
      "password" -> text
    )((username, email, password) => User(java.util.UUID.randomUUID(), username, email, password))
     ((user: User) => Some(user.name, user.email, ""))
  )

  val loginForm = Form(
    tuple(
      "id" -> text,
      "password" -> text
  ))

  val bookForm = Form[Book](
    mapping(
      "title" -> text,
      "authors" -> list(text),
      "languages" -> list(text),
      "publishers" -> list(text),
      "published_dates" -> list(text),
      "tags" -> list(text),
      "summary" -> text
    )((title,authors,languages,publishers,published_dates,tags,summary) =>
      Book(null,title,authors,languages,publishers,published_dates, tags, summary, List[BookPart]()))
    ((book:Book) => Some(book.bookTitle, book.bookAuthors, book.bookLanguages, book.bookPublishers, book.bookPublishedDates,
                      book.bookTags, book.bookSummary))
  )

  val memberForm = Form(
    tuple(
      "member id" -> text,
      "role" -> text
    )
  )

  def ezoomlayerForm(ezoombookid:UUID, layerid:UUID, userid:UUID) = Form[EzoomLayer](
    mapping(
      "ezoomlayer_id" -> ignored(layerid),
      "ezoombook_id" -> ignored(ezoombookid),
      "ezoomlayer_level" -> default(number, 1),
      "ezoomlayer_owner" -> ignored("user:"+userid),
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

  def contribMapping(partId:String,ezlId:UUID,ezbId:UUID,uid:UUID):Mapping[Contrib] = mapping(
    "contrib_id" -> ignored(UUID.randomUUID().toString),
    "contrib_type" -> text,
    "ezoomlayer_id" -> ignored(ezlId),
    "ezoombook_id" -> ignored(ezbId),
    "user_id" -> ignored(uid),
    "contrib_part" -> ignored(partId),
    "contrib_status" -> default[Status.Value](of[Status.Value], Status.workInProgress),
    "contrib_locked" -> default(boolean, false),
    "contrib_content" -> default(text, ""),
    "part_title" -> optional(text),
    "part_summary" -> optional(text),
    "part_contribs" -> optional(list(atomicContribMapping(ezlId,ezbId,uid,partId)))
  )(contribApply)(contribUnapply)

  def contribMapping:Mapping[Contrib] = mapping(
    "contrib_id" -> text,
    "contrib_type" -> text,
    "ezoomlayer_id" -> of[UUID],
    "ezoombook_id" -> of[UUID],
    "user_id" -> of[UUID],
    "contrib_part" -> text,
    "contrib_status" -> of[Status.Value],
    "contrib_locked" -> boolean,
    "contrib_content" -> text,
    "part_title" -> optional(text),
    "part_summary" -> optional(text),
    "part_contribs" -> optional(list(atomicContribMapping))
  )(contribApply)(contribUnapply)

  def atomicContribMapping:Mapping[AtomicContrib] = mapping(
    "contrib_id" -> text,
    "contrib_type" -> text,
    "ezoomlayer_id" -> of[UUID],
    "ezoombook_id" -> of[UUID],
    "user_id" -> of[UUID],
    "contrib_part" -> text,
    "contrib_status" -> of[Status.Value],
    "contrib_locked" -> boolean,
    "contrib_content" -> text
  )(AtomicContrib.apply)(AtomicContrib.unapply)

  def atomicContribMapping(ezlid:UUID,ezbid:UUID,uid:UUID,partid:String):Mapping[AtomicContrib] = mapping(
    "contrib_id" -> ignored(UUID.randomUUID().toString),
    "contrib_type" -> text,
    "ezoomlayer_id" -> ignored(ezlid),
    "ezoombook_id" -> ignored(ezbid),
    "user_id" -> ignored(uid),
    "contrib_part" -> ignored(partid),
    "contrib_status" -> default[Status.Value](of[Status.Value], Status.workInProgress),
    "contrib_locked" -> default(boolean, true),
    "contrib_content" -> text
  )(AtomicContrib.apply)(AtomicContrib.unapply)

  def contribApply(
    contrib_id: String,
    contrib_type: String,
    ezoomlayer_id: UUID,
    ezoombook_id: UUID,
    user_id: UUID,
    part_id: String,
    contrib_status: Status.Value,
    contrib_locked: Boolean,
    contrib_content: String,
    part_title: Option[String],
    part_summary: Option[String],
    part_contribs: Option[List[AtomicContrib]]):Contrib = contrib_type match{
      case "contrib.Part" =>
        EzlPart(contrib_id,ezoomlayer_id,ezoombook_id,user_id,part_id,contrib_status,contrib_locked,
          part_title.getOrElse(""),part_summary.getOrElse(""),part_contribs.getOrElse(List[AtomicContrib]()))
      case _ =>
        AtomicContrib(contrib_id,contrib_type,ezoomlayer_id,ezoombook_id,
          user_id,part_id,contrib_status,contrib_locked,contrib_content)
  }

  def contribUnapply(contrib:Contrib) = Some(
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
      case part:EzlPart => Some(part.part_summary)
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