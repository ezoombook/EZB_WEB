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
      "id" -> ignored(layerid),
      "ezoombook_id" -> ignored(ezoombookid),
      "level" -> default(number, 1),
      "owner" -> ignored("user:"+userid),
      "status" -> default[Status.Value](of[Status.Value], Status.workInProgress),
      "locked" -> default(boolean, true),
      "ezl_summaries" -> list(text),
      "contribs" -> list(
         mapping(
             "contrib_id" -> text,
             "contrib_type" -> text,
             "ezoomlayer_id" -> ignored(layerid),
             "ezoombook_id" -> ignored(ezoombookid),
             "user_id" -> ignored(userid),
             "contrib_part" -> text,
             "contrib_status" -> default[Status.Value](of[Status.Value], Status.workInProgress),
             "contrib_locked" -> boolean,
             "contrib_content" -> text
         )(Contrib.apply)(Contrib.unapply)
      )
    )(EzoomLayer.apply)(EzoomLayer.unapply)
  )

  def ezoomlayerForm = Form[EzoomLayer](
    mapping(
      "id" -> of[UUID],
      "ezb_id" -> of[UUID],
      "level" -> number,
      "owner" -> text,
      "status" -> of[Status.Value],
      "locked" -> boolean,
      "ezl_summaries" -> list(text),
      "contribs" -> list(
        mapping(
          "contrib_id" -> text,
          "contrib_type" -> text,
          "ezoomlayer_id" -> of[UUID],
          "ezoombook_id" -> of[UUID],
          "user_id" -> of[UUID],
          "contrib_part" -> text,
          "contrib_status" -> of[Status.Value],
          "contrib_locked" -> boolean,
          "contrib_content" -> text
        )(Contrib.apply)(Contrib.unapply)
      )
    )(EzoomLayer.apply)
      (EzoomLayer.unapply)
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
