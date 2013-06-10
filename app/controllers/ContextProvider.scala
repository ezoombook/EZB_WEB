package controllers

import users.dal.User
import models.Context

import play.api.mvc.Request

import java.util.UUID

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 04/06/13
 * Time: 15:33
 * To change this template use File | Settings | File Templates.
 */
trait ContextProvider {
  implicit def context[A](implicit request:Request[A]) : Context = {
    val userId = request.session.get("userId")
    val userName = request.session.get("userName")
    val userMail = request.session.get("userMail")

    val user = userId.flatMap(uid =>
      userName.flatMap(name =>
        userMail.map(mail =>
          User(UUID.fromString(uid),name,mail,"****")
        )
      )
    )

    Context(user)
  }
}
