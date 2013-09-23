package controllers

import models._
import users.dal._
import books.dal._
import project.dal._
import forms.{AppForms, EzbForms}
import AppForms._
import EzbForms._
import play.api.data.Forms._


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

import javax.mail._
import javax.mail.internet._
import java.util.Properties._

object Application extends Controller with ContextProvider {

  protected val HOME_URL = "/"

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

  val contactForm = Form(
    tuple(
      "usermail" -> text,
      "arequest" -> text
    )

  )

  val passwordForm = Form(
    tuple(
      "password1" -> text,
      "password2" -> text
    )

  )

  val passwordrForm = Form(
    tuple(
      "email" -> text,
      "password" -> text
    )

  )

  val localeForm = Form("locale" -> nonEmptyText)

  def faq = Action {
    implicit request =>
      Ok(views.html.faq())
  }

  def contact = Action {
    implicit request =>
      Ok(views.html.contact(contactForm))
  }

  def generalerror = Action {
    implicit request =>
      Ok(views.html.generalerror())
  }


  def forum = Action {
    implicit request =>
      Ok(views.html.forum())
  }

  def asearch = Action {
    implicit request =>
      Ok(views.html.asearch(List[Book]()))
  }

  def truehome = Action {
    implicit request =>
      Ok(views.html.index(BookDO.listBooks))
  }

  def tutorial = Action {
    implicit request =>
      Ok(views.html.tutorial())
  }

  def index = Action {
    implicit request =>
      Ok(views.html.index(BookDO.listBooks))
  }

  def errorlogin = Action {
    implicit request =>
      Ok(views.html.errorlogin())

  }

  /**
   * Adds a new user
   */
  def newUser = Action {
    implicit request =>
      userForm.bindFromRequest.fold(
        errors => {
          BadRequest(views.html.login(loginForm, errors))
        },
        user => {
          UserDO.create(user)
          UserDO.getUser(user.name).map {
            newusr =>
              Redirect(routes.Application.home).withSession(
                "userId" -> newusr.id.toString,
                "userName" -> newusr.name,
                "userMail" -> newusr.email
              )
          }.getOrElse {
            println("[ERROR] Could not find user " + user.name)
            BadRequest(views.html.login(loginForm, userForm))
          }
        }
      )
  }

  /**
   * Validates the user login form and creates a session
   */
  def validate = Action {
    implicit request =>
      println("[INFO] Login validation...")
      loginForm.bindFromRequest.fold(
        errors => {
          println("[ERROR] Form error while validating user: " + errors)
          BadRequest(views.html.login(errors, userForm))
        },
        up => {
          if (UserDO.validateUser(up._1, up._2)) {
            UserDO.getUser(up._1).map {
              user =>
                println("[INFO] Loged-in as user " + user.name)
                // Get preferences
                val prefs = UserDO.getUserPreferences(user.id)

                Redirect(routes.Application.home).withSession(
                  "userId" -> user.id.toString,
                  "userName" -> user.name,
                  "userMail" -> user.email,
                  "maxHistory" -> prefs.map(_.maxHistory.toString).getOrElse("10"),
                  "language" -> prefs.map(_.language).getOrElse("")
                )
            }.getOrElse {
              println("[ERROR] Could not find user " + up._1)
              BadRequest(views.html.login(loginForm, userForm))
            }
          } else {
            println("[ERROR] Invalid user credentials for user : " + up._1)
            BadRequest(views.html.login(loginForm, userForm))
          }
        }
      )
  }

  /**
   * Displays the form for sending the reset-password link
   */
  def changePassword = Action {
    implicit request =>
      Ok(views.html.forgottenPwd(Form("email" -> email)))
  }

  /**
   * Creates a temporal link to reset the user password and sends it to the user by email
   * @return
   */
  def sendPasswordResetLink = Action {
    implicit request =>
      import AppDB._
      Form("email" -> email).bindFromRequest.fold(
        errors => BadRequest(views.html.forgottenPwd(errors)),
        userEmail => {
          UserDO.getUserId(userEmail).map {
            uid =>
              val id = utils.MD5Util.md5Hex(userEmail + (new java.util.Date()).getTime)
              AppDB.storeTemporalLinkId(id, uid.toString)
              println("voic: " + id)
              // Set up the mail object
              val properties = System.getProperties
              properties.put("mail.smtp.host", "localhost")
              val session = javax.mail.Session.getDefaultInstance(properties)
              val message = new MimeMessage(session)

              // Set the from, to, subject, body text
              message.setFrom(new InternetAddress("ezoombook@laposte.net"))
              message.setRecipients(Message.RecipientType.TO, userEmail)
              message.setSubject("Greetings from langref.org")
              message.setText(id)

              // And send it
              //Transport.send(message)

              Unauthorized("We have sent you a link to reset your password.")
          }.getOrElse {
            Unauthorized("Ooops! The mail you provided does not appear in our dabase.")
          }
        })
  }

  def contactadmin = Action {
    implicit request =>
      import AppDB._
      contactForm.bindFromRequest.fold(
        errors => {
          println("bad:" + errors)
          BadRequest(views.html.contact(errors))
        },
        (contactform) => {
          // Set up the mail object
          val properties = System.getProperties
          properties.put("mail.smtp.host", "localhost")
          val session = javax.mail.Session.getDefaultInstance(properties)
          val message = new MimeMessage(session)

          // Set the from, to, subject, body text
          message.setFrom(new InternetAddress("ezoombook@laposte.net"))
          message.setRecipients(Message.RecipientType.TO, "ezoombook@laposte.net")
          message.setSubject("Message from" + contactform._1)
          message.setText(contactform._2)

          // And send it
          Transport.send(message)
          Ok("contact saved")
        })
  }

  /**
   * Changes the password from the parameter page
   * @return
   */
  def changepass = Action {
    implicit request =>
      import AppDB._
      passwordForm.bindFromRequest.fold(
        errors => {
          println("bad:" + errors)
          BadRequest(views.html.parameter(errors, localeForm, ""))
        },
        (passwordform) => {
          if (passwordform._1 == passwordform._2 && passwordform._1 != "") {
            UserDO.changePassword(context.user.get.id, passwordform._2)
            Ok(views.html.parameter(passwordForm, localeForm, "sucess"))
          } else {
            var error = "Mismatch password"

            Ok(views.html.parameter(passwordForm, localeForm, error))
          }
        })
  }

  /**
   * Changes the password from the "forgot your password" link
   * @return
   */
  def changepasswo = Action {
    implicit request =>
      import AppDB._
      passwordrForm.bindFromRequest.fold(
        errors => {
          println("bad:" + errors)
          BadRequest(views.html.passwordReset("", errors, ""))
        },
        (passwordrform) => {
          if (passwordrform._2 != "") {


            UserDO.getUserId(passwordrform._1).map {
              uid =>
                UserDO.changePassword(uid, passwordrform._2)
            }.getOrElse(
              Unauthorized("Oops! that is not a valid page!")
            )


            Ok(views.html.passwordReset("", passwordrForm, "sucess"))
          } else {
            var error = "Mismatch password"

            Ok(views.html.passwordReset("", passwordrForm, error))
          }
        })
  }

  /**
   * Validates that the link is still valid. i.e, hasn't been used or it's not expired
   * and redirects the user to the change password view
   * @return
   */
  def passwordReset(linkId: String) = Action {
    implicit request =>
      import AppDB._
      AppDB.getTemporalLinkId(linkId).map {
        uid =>
          Ok(views.html.passwordReset(uid, passwordrForm, ""))
      }.getOrElse(
        Unauthorized("Ooops! that is not a valid page!")
      )
  }

  /**
   * Displays the user home page
   */

  def home = Action {
    implicit request =>
      context.user.map {
        u =>
          val listproj = BookDO.getOwnedProjects(u.id).foldLeft(List[(EzbProject, Ezoombook)]()) {
            (list, proj) =>
              BookDO.getEzoomBook(proj.ezoombookId).map {
                ezb =>
                  list :+(proj, ezb)
              }.getOrElse {
                list
              }
          }
          val listpro = BookDO.getProjectsByMember(u.id).foldLeft(List[(EzbProject, Ezoombook)]()) {
            (list, proj) =>
              BookDO.getEzoomBook(proj.ezoombookId).map {
                ezb =>
                  list :+(proj, ezb)
              }.getOrElse {
                list
              }
          }
          Ok(views.html.workspace(listproj, listpro, BookDO.getUserEzoombooks(u.id), BookDO.getUserBooks(u.id), UserDO.userOwnedGroups(u.id), UserDO.userIsMemberGroups(u.id), groupForm))
      }.getOrElse(
        Unauthorized("Oops, you are not connected")
      )

  }

  /**
   * Displays the login form
   */
  def login = Action {
    implicit request =>
      Ok(views.html.login(loginForm, userForm))
  }

  /**
   * Logout the user
   */
  def userLogout = Action {
    implicit request =>

      Redirect(routes.Application.login).withNewSession
  }

  def parameter = Action { implicit request =>
    withUser{user =>
      val lang = context.preferences.map(_.language).getOrElse("")
      Ok(views.html.parameter(passwordForm, localeForm.bind(Map("locale" -> lang)), ""))
    }
  }

  /**
   * Changes the user predefined language
   */
  def changeLang = Action{implicit request =>
    val referer = request.headers.get(REFERER).getOrElse(HOME_URL)
    localeForm.bindFromRequest.fold(
      erros => {
        BadRequest(referer)
      },
      locale => {
        println("[INFO] Language changed to " + locale)
        Redirect(referer).withLang(play.api.i18n.Lang(locale)).withSession(
          session + ("language" -> locale)
        )
      }
    )
  }

  /**
   * Changes session language
   */
  def setLang(langCode:String) = Action{implicit request =>
    val referer = request.headers.get(REFERER).getOrElse(HOME_URL)
    println("[INFO] Language changed to " + langCode)
    Redirect(referer).withLang(play.api.i18n.Lang(langCode)).withSession(
      session + ("language" -> langCode)
    )
  }
}
