package controllers

import models._
import users.dal._

import play.api._
import play.api.mvc._
import play.api.data._
import Forms._

object Application extends Controller {
  
  val userForm = Form(
    mapping(
      "username" -> text,
      "mail" -> email,
      "password" -> text
    )((username, email, password) => User(java.util.UUID.randomUUID(), username, email, password))
     ((user: User) => Some(user.name, user.email, "****"))
  )

  def index = Action {
    Redirect(routes.Application.users)
  }
  
  def users = Action {
    Ok(views.html.index(UserDO.listUsers, userForm))
  }

  def newUser = Action { implicit request =>
    userForm.bindFromRequest.fold(
      errors => {
	BadRequest(views.html.index(UserDO.listUsers, errors))
      },
      user => {
	UserDO.create(user)
	Redirect(routes.Application.users)
      }
    )
  }
}
