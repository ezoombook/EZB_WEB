package controllers

import users.dal._
import books.dal.{Ezoombook, EzoomLayer}
import models.Context

import play.api.mvc.{SimpleResult, Request, Controller}
import play.api.cache
import cache.Cache
import play.api.Play.current

import java.util.UUID
import scala.concurrent.Future

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 04/06/13
 * Time: 15:33
 * To change this template use File | Settings | File Templates.
 */
trait ContextProvider {
  //extends Controller{
  this: Controller =>

  val WORKING_EZB = "working-ezb"
  val WORKING_LAYER = "working-layer"

  implicit def context[A](implicit request: Request[A]): Context = {
    val userId = request.session.get("userId")
    val userName = request.session.get("userName")
    val userMail = request.session.get("userMail")

    val user = userId.flatMap(uid =>
      userName.flatMap(name =>
        userMail.map(mail =>
          User(UUID.fromString(uid), name, mail, "****")
        )
      )
    )

    val prefs = user.flatMap(u =>
      request.session.get("maxHistory").flatMap(maxHistory =>
        request.session.get("language").map(language =>
          Preferences(u.id, maxHistory.toInt, language)
        )
      ))

    val ezb = request.session.get(WORKING_EZB).map(UUID.fromString(_))

    val layer = request.session.get(WORKING_LAYER).map(UUID.fromString(_))

    Context(user, prefs, request.acceptLanguages, ezb, layer)
  }

  def withUser[A](block: (User) => SimpleResult)(implicit request: Request[A]): SimpleResult = {
    context.user.map {
      user =>
        block(user)
    }.getOrElse {
      Unauthorized("Oops! you need to be connected to acccess this page.")
    }
  }

  def withUserAsync[A](block: (User) => Future[SimpleResult])(implicit request: Request[A]): Future[SimpleResult] = {
    context.user.map {
      user =>
        block(user)
    }.getOrElse {
      Future.successful(Unauthorized("Oops! you need to be connected to acccess this page."))
    }
  }

}
