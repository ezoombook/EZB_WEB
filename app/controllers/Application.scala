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
import play.api.cache.Cache
import Forms._

import Play.current

import java.io.ByteArrayInputStream
import java.util.UUID
import play.api.libs.json.Json

import javax.mail._
import javax.mail.internet._
import java.util.Properties._

import jp.t2v.lab.play2.auth.{LoginLogout, OptionalAuthElement}

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

object Application extends Controller with LoginLogout with OptionalAuthElement with AuthConfigImpl with ContextProvider {

  val HOME_URL = "/"

  def index = StackAction {
    implicit request =>
      Ok(views.html.index(BookDO.listBooks))
  }

  /**
   * Validates the user login form and creates a session
   */
  def validate = Action.async {
    implicit request =>
      Logger.debug("Login validation...")
      loginForm.bindFromRequest.fold(
        errors => {
          Logger.error("Form error while validating user: " + errors)
          Future.successful(BadRequest(views.html.login(errors, userForm)))
        },
        user => {
          Logger.debug("Validation Succeeded")
          val prefs = UserDO.getUserPreferences(user.get.id)
            gotoLoginSucceeded(user.get.id).map(_.withSession(
              "userId" -> user.get.id.toString,
              "userName" -> user.get.name,
              "userMail" -> user.get.email,
              "maxHistory" -> prefs.map(_.maxHistory.toString).getOrElse("10"),
              "language" -> prefs.map(_.language).getOrElse("")
            ))
        }
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
  def logout = Action.async {
    implicit request =>
      Cache.remove("logged-user")
        gotoLogoutSucceeded
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
          if (UserDO.create(user) > 0) {
            Redirect(routes.Application.login)
          } else {
            println("[ERROR] Could not create user " + user.name)
            BadRequest(views.html.login(loginForm, userForm.withGlobalError("Could not create user.")))
          }
        }
      )
  }

  /**
   * Changes session language
   */
  def setLang(langCode: String) = Action {
    implicit request =>
      val referer = request.headers.get(REFERER).getOrElse(HOME_URL)
      println("[INFO] Language changed to " + langCode)
      Redirect(referer).withLang(play.api.i18n.Lang(langCode)).withSession(
        session + ("language" -> langCode)
      )
  }

}
