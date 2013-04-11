package controllers

import models._
import users.dal._
import books.dal._
import utils.Implicits._

import play.api.data._
import play.api.cache
import cache.Cache
import Forms._
import java.util.UUID

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
}
