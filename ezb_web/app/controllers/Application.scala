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
println("[INFO] Login validation...")
    loginForm.bindFromRequest.fold(
      errors => {
        println("[ERROR] Form error while validating user: " + errors)
	      BadRequest(views.html.login(errors, UserDO.listUsers, userForm ))
      },
      up => {
      	if (UserDO.validateUser(up._1, up._2)){
	          UserDO.getUser(up._1).map{user =>
              println("[INFO] Loged-in as user " + user.name)
              Redirect(routes.Application.home).withSession(
                "userId" -> user.id.toString,
                "userName" -> user.name,
                "userMail" -> user.email
              )
            }.getOrElse{
              println("[ERROR] Could not find user " + up._1)
	            BadRequest(views.html.login(loginForm, UserDO.listUsers, userForm ))
            }
        }else{
          println("[ERROR] Invalid user credentials for user : " + up._1)
	        BadRequest(views.html.login(loginForm, UserDO.listUsers, userForm ))
        }
      }
    )
  }

  /**
   * Displays the form for sending the reset-password link
   */
  def changePassword = Action { implicit request =>
    Ok(views.html.forgottenPwd(Form("email" -> email)))
  }

  /**
   * Creates a temporal link to reset the user password and sends it to the user by email
   * @return
   */
  def sendPasswordResetLink = Action {implicit request =>
    import AppDB._
    Form("email" -> email).bindFromRequest.fold(
      errors => BadRequest(views.html.forgottenPwd(errors)),
      userEmail => {
        UserDO.getUserId(userEmail).map{uid =>
          val id = utils.MD5Util.md5Hex(userEmail + (new java.util.Date()).getTime)
          AppDB.storeTemporalLinkId(id, uid.toString)
          //TODO Actually send the email
          Unauthorized("We have sent you a link to reset your password.")
        }.getOrElse{
        Unauthorized("Ooops! The mail you provided does not appear in our dabase.")
        }
      })
  }

  /**
   * Validates that the link is still valid. i.e, hasn't been used or it's not expired
   * and redirects the user to the change password view
   * @return
   */
  def passwordReset(linkId:String) = Action {implicit request =>
    import AppDB._
    AppDB.getTemporalLinkId(linkId).map{uid =>
      Ok(views.html.passwordReset(uid))
    }.getOrElse(
      Unauthorized("Ooops! that is not a valid page!")
    )
  }

  /**
   * Displays the user home page
   */
  def home = Action { implicit request =>
    context.user.map{u =>
      Ok(views.html.workspace(UserDO.userOwnedGroups(u.id), UserDO.userIsMemberGroups(u.id),groupForm))
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
