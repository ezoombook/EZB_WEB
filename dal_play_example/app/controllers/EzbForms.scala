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

  val bookForm = Form(
    mapping(
      "id" -> text,
      "title" -> text,
      "authors" -> list(text),
      "languages" -> list(text),
      "publishers" -> list(text),
      "published_dates" -> list(text),
      "tags" -> list(text),
      "summary" -> text,
      "parts" -> list(text)
    )((id,title,authors,languages,publishers,published_dates,tags,summary,parts) =>
      Book(id,title,authors,languages,publishers,published_dates, tags, summary, parts.map(p=>BookPart(p,id,Array[Byte]()))))
    ((book:Book) => Some(book.bookId, book.bookTitle, book.bookAuthors, book.bookLanguages, book.bookPublishers, book.bookPublishedDates,
                      book.bookTags, book.bookSummary, book.bookParts.map(_.partId)))
  )

  val memberForm = Form(
    tuple(
      "member id" -> text,
      "role" -> text
    )
  )
}
