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

import java.util.UUID
import javax.mail._
import javax.mail.internet._
import java.util.Properties._

import jp.t2v.lab.play2.auth.AuthElement

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 30/10/2013
 * Time: 11:55
 * To change this template use File | Settings | File Templates.
 */
object Workspace extends Controller with AuthElement with AuthConfigImpl with ContextProvider{

  val groupForm = Form(
    tuple(
      "groupName" -> text,
      "ownerId" -> text
    )
  )

  /**
   * Displays the user home page
   */
  def home = StackAction(AuthorityKey -> RegisteredUser) {
    implicit request =>
      withUser {
        user =>
          Ok(views.html.workspace(
            Collaboration.getProjectsByUser(user.id),
            BookDO.getUserEzoombooks(user.id),
            BookDO.getUserBooks(user.id),
            UserDO.userOwnedGroups(user.id),
            UserDO.userIsMemberGroups(user.id), groupForm))
      }
  }

  def parameter = StackAction(AuthorityKey -> RegisteredUser) {
    implicit request =>
      withUser {
        user =>
          val lang = context.preferences.map(_.language).getOrElse("")
          Ok(views.html.parameter(passwordForm, localeForm.bind(Map("locale" -> lang)), ""))
      }
  }

  /**
   * Changes the user predefined language
   */
  def changeLang = StackAction(AuthorityKey -> RegisteredUser) {
    implicit request =>
      val referer = request.headers.get(REFERER).getOrElse(Application.HOME_URL)
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

}
