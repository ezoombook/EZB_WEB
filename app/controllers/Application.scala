package controllers

import models._
import users.dal._
import books.dal._
import forms.{AppForms, EzbForms}
import AppForms._
import EzbForms._

import play.api._
import play.api.mvc._
import play.api.data._
import Forms._

import Play.current

import java.io.ByteArrayInputStream
import java.util.UUID
import play.api.libs.json.Json
import users.dal.User
import scala.Some

object Application extends Controller with ContextProvider{

  val userForm = Form(
    mapping(
      "username" -> text,
      "mail" -> email,
      "password" -> text
    )((username, email, password) => User(java.util.UUID.randomUUID(), username, email, password))
      ((user: User) => Some(user.name, user.email, ""))
  )


  
      val groupForm = Form(
    tuple(
      "groupName" -> text,
      "ownerId" -> text
    )
  
    )
    
    
       def faq = Action {implicit request =>
    Ok(views.html.faq())
       }
       
       def contact = Action {implicit request =>
    Ok(views.html.contact())
       }
       
       def asearch = Action {implicit request =>
    Ok(views.html.asearch(List[Book]()))
       }
    
       def truehome = Action {implicit request =>
    Ok(views.html.truehome(List[Book]()))
       }

def tutorial = Action {implicit request =>
    Ok(views.html.tutorial())
  }
        
    
  def index = Action {
    Redirect(routes.Application.login)
  }
  


  /**
   * Lists existing users
   * @return
   */
  def users = Action {implicit request =>
    Ok(views.html.index(UserDO.listUsers, userForm))
  }

  /**
   * Adds a new user
   */
  def newUser = Action { implicit request =>
    userForm.bindFromRequest.fold(
      errors => {
      	BadRequest(views.html.index(UserDO.listUsers, errors))
      },
      user => {
        UserDO.create(user)
        Redirect(routes.Application.login)
      }
    )
  }

  /**
   * Validates the user login form and creates a session
   */
  def validate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      errors => {
	      BadRequest(views.html.login(errors, UserDO.listUsers, userForm ))
      },
      up => {
      	if (UserDO.validateUser(up._1, up._2))
	          UserDO.getUser(up._1).map{uid =>
              Redirect(routes.Application.home).withSession(
                "userId" -> uid.toString,
                "userName" -> up._1
              )
            }.getOrElse(
	            BadRequest(views.html.login(loginForm, UserDO.listUsers, userForm ))
	          )
	      else
	        BadRequest(views.html.login(loginForm, UserDO.listUsers, userForm ))
      }
    )
  }

  /**
   * Displays the user home page
   */
  def home = Action { implicit request =>
    session.get("userId").map(UUID.fromString(_)).map{uid => 
      Ok(views.html.workspace(UserDO.userOwnedGroups(uid), UserDO.userIsMemberGroups(uid),groupForm))
    }.getOrElse(
	    Unauthorized("Oops, you are not connected")
    )
  }

  /**
   * Displays the login form
   */
  def login = Action{ implicit request =>
    Ok(views.html.login(loginForm, UserDO.listUsers, userForm ))
  }

  /**
  * Logout the user
  */
  def userLogout = Action { implicit request =>
		
		Redirect(routes.Application.login).withNewSession
	}
	
	 def parameter = Action{ implicit request =>
    Ok(views.html.parameter())
  }
	
}
