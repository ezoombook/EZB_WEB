package controllers

import users.dal._
import models._
import forms.{AppForms, EzbForms}
import AppForms._
import EzbForms._
import play.api.data.Forms._

import play.api._
import scala.Some
import Play.current
import cache.Cache

import java.io._
import java.util.UUID
import play.api._
import play.api.mvc._
import play.api.data._
import Forms._
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.i18n.Messages
import org.apache.commons.io.IOUtils
import scala.xml.XML
import jp.t2v.lab.play2.auth.AuthElement
import books.dal.Book
import models.{UserDO, AppDB}

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 30/10/2013
 * Time: 16:18
 * To change this template use File | Settings | File Templates.
 */
object Support extends Controller with AuthElement with AuthConfigImpl with ContextProvider{

  def faq = StackAction(AuthorityKey -> Guest) {
    implicit request =>
      Ok(views.html.faq())
  }

  def contact = StackAction(AuthorityKey -> Guest) {
    implicit request =>
      Ok(views.html.contact(contactForm))
  }

  def forum = StackAction(AuthorityKey -> Guest) {
    implicit request =>
      Ok(views.html.forum())
  }

  def asearch = StackAction(AuthorityKey -> Guest) {
    implicit request =>
      Ok(views.html.asearch(List[Book]()))
  }

  def tutorial = StackAction(AuthorityKey -> Guest) {
    implicit request =>
      Ok(views.html.tutorial())
  }

  def contactadmin = StackAction(AuthorityKey -> RegisteredUser) {
    implicit request =>
      import AppDB._
      contactForm.bindFromRequest.fold(
        errors => {
          println("bad:" + errors)
          BadRequest(views.html.contact(errors))
        },
        (contactform) => {
//          // Set up the mail object
//          val properties = System.getProperties
//          properties.put("mail.smtp.host", "localhost")
//          val session = javax.mail.Session.getDefaultInstance(properties)
//          val message = new MimeMessage(session)
//
//          // Set the from, to, subject, body text
//          message.setFrom(new InternetAddress("ezoombook@laposte.net"))
//          message.setRecipients(Message.RecipientType.TO, "ezoombook@laposte.net")
//          message.setSubject("Message from" + contactform._1)
//          message.setText(contactform._2)
//
//          // And send it
//          Transport.send(message)
          Ok("contact saved")
        })
  }

  /**
   * Changes the password from the parameter page
   * @return
   */
  def changepass = StackAction(AuthorityKey -> RegisteredUser) {
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
   * Displays the form for sending the reset-password link
   */
  def changePassword = StackAction(AuthorityKey -> Guest) {
    implicit request =>
      Ok(views.html.forgottenPwd(Form("email" -> email)))
  }

  /**
   * Creates a temporal link to reset the user password and sends it to the user by email
   * @return
   */
  def sendPasswordResetLink = StackAction(AuthorityKey -> Guest) {
    implicit request =>
      import AppDB._
      Form("email" -> email).bindFromRequest.fold(
        errors => BadRequest(views.html.forgottenPwd(errors)),
        userEmail => {
          UserDO.getUserId(userEmail).map {
            uid =>
//              val id = utils.MD5Util.md5Hex(userEmail + (new java.util.Date()).getTime)
//              AppDB.storeTemporalLinkId(id, uid.toString)
//              println("voic: " + id)
//              // Set up the mail object
//              val properties = System.getProperties
//              properties.put("mail.smtp.host", "localhost")
//              val session = javax.mail.Session.getDefaultInstance(properties)
//              val message = new MimeMessage(session)
//
//              // Set the from, to, subject, body text
//              message.setFrom(new InternetAddress("ezoombook@laposte.net"))
//              message.setRecipients(Message.RecipientType.TO, userEmail)
//              message.setSubject("Greetings from langref.org")
//              message.setText(id)
//
//              // And send it
//              //Transport.send(message)

              Unauthorized("We have sent you a link to reset your password.")
          }.getOrElse {
            Unauthorized("Ooops! The mail you provided does not appear in our dabase.")
          }
        })
  }


  /**
   * Changes the password from the "forgot your password" link
   * @return
   */
  def changepasswo = StackAction(AuthorityKey -> Guest) {
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
  def passwordReset(linkId: String) = StackAction(AuthorityKey -> Guest) {
    implicit request =>
      import AppDB._
      AppDB.getTemporalLinkId(linkId).map {
        uid =>
          Ok(views.html.passwordReset(uid, passwordrForm, ""))
      }.getOrElse(
        Unauthorized("Ooops! that is not a valid page!")
      )
  }

}
