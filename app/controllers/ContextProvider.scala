package controllers

import models.Context

import play.api.mvc.Request

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 04/06/13
 * Time: 15:33
 * To change this template use File | Settings | File Templates.
 */
trait ContextProvider {
  implicit def context[A](implicit request:Request[A]) : Context = {
    Context(request.session.get("userId"))
  }
}
