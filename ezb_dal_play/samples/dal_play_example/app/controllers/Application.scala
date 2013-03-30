package controllers

import models._
import users.dal._

import play.api._
import play.api.mvc._
import play.api.data._
import Forms._

import java.util.UUID

object Application extends Controller {
  
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

  def validate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      errors => {
	BadRequest(views.html.login(errors))
      },
      up => {
	if (UserDO.validateUser(up._1, up._2))
	  UserDO.getUser(up._1).map(uid =>	
	    Ok(views.html.workspace(UserDO.listBooks(uid), bookForm)).withSession(
	      "userId" -> uid.toString,
	      "userName" -> up._1
	    )
	  ).getOrElse(
	    BadRequest(views.html.login(loginForm))
	  )
	else	       
	  BadRequest(views.html.login(loginForm))
      }
    )
  }

  def login = Action{ implicit request =>
    Ok(views.html.login(loginForm))
  }

  def newBook = Action{ implicit request =>    
    bookForm.bindFromRequest.fold(
      errors => {
	BadRequest(views.html.workspace(List[(String,Long)](), errors))
      },
      book => {
	session.get("userId").map(UUID.fromString(_)).map{uid =>
	  UserDO.newUserBook(uid, UUID.fromString(book._2))
	  Ok(views.html.workspace(UserDO.listBooks(uid), bookForm))
	}.getOrElse(
	  Unauthorized("Oops, you are not connected")
	)
      }
    )
  }

}
