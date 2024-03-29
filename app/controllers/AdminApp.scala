package controllers

import models._
import users.dal._

import play.api._
import play.api.mvc._
import play.api.data._
import Forms._
import jp.t2v.lab.play2.auth.AuthElement

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

import java.util.UUID

object AdminApp extends Controller with AuthElement with AuthConfigImpl with ContextProvider{
  var iscreateGroups = true
  var iscreateBooks = true

  val users = List( User(java.util.UUID.randomUUID(), "bob", "bob@ezb.com", "bob123"),
		    User(java.util.UUID.randomUUID(), "ann", "ann@ezb.com", "ann101"),
		    User(java.util.UUID.randomUUID(), "dal", "dal@ezb.com", "dal101"),
		    User(java.util.UUID.randomUUID(), "mat", "mat@ezb.com", "mat101"))

  val groups = List(
    ("group A", users(0).id),
    ("group B", users(3).id),
    ("group C", users(2).id)
  )

  def index = StackAction(AuthorityKey -> Administrator) {implicit request =>
    Ok(views.html.admin(UserDO.listUsers, iscreateGroups, iscreateBooks))
  }

  def createUsers = StackAction(AuthorityKey -> Administrator) {implicit request =>
    for (u <- users){
      UserDO.create(u)
    }
    
    Ok(views.html.admin(UserDO.listUsers, iscreateGroups, iscreateBooks))
  }

  def createGroups = StackAction(AuthorityKey -> Administrator) {implicit request =>
    for(g <- groups){
      UserDO.newGroup(g._1, g._2)
    }

    iscreateGroups = false

    Ok(views.html.admin(UserDO.listUsers, iscreateGroups, iscreateBooks))
  }

  def createBooks = StackAction(AuthorityKey -> Administrator) {implicit request =>
    UserDO.newUserBook(users(0).id, UUID.randomUUID)
    UserDO.newUserBook(users(1).id, UUID.randomUUID)
    UserDO.newUserBook(users(1).id, UUID.randomUUID)
    UserDO.newUserBook(users(3).id, UUID.randomUUID)

    iscreateBooks = false

    Ok(views.html.admin(UserDO.listUsers, iscreateGroups, iscreateBooks))
  }

}
