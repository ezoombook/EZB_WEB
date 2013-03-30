package controllers

import models._
import users.dal._
import books.dal._

import play.api.data._
import Forms._

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
    tuple(
      "title" -> text,
      "book id" -> text
    )
  )

  val memberForm = Form(
    tuple(
      "member id" -> text,
      "role" -> text
    )
  )
}
