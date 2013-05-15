package controllers

import models._
import users.dal._
import books.dal._

import play.api._
import users.dal.Group
import users.dal.User
import scala.Some
import cache.Cache

import Play.current

import play.api._
import play.api.mvc._
import play.api.data._
import Forms._
import java.util.UUID


/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 15/05/13
 * Time: 16:31
 * To change this template use File | Settings | File Templates.
 */

/**
 * Manage community related operations: groups, etc
 *
 */
object Community extends Controller{

  val memberForm = Form(
    tuple(
      "member id" -> text,
      "role" -> text
    )
  )

  /**
   * Displays the groups owned by a user
   */
  def groups = Action{ implicit request =>
    session.get("userId").map(UUID.fromString(_)).map{uid =>
      Ok(views.html.groups(UserDO.userOwnedGroups(uid), UserDO.userIsMemberGroups(uid)))
    }.getOrElse(
      Unauthorized("Oops, you are not connected")
    )
  }

  /**
   * Displays the deails of a group
   */
  def group(groupId:String) = Action{ implicit request =>
    cachedGroup(groupId).map{group =>
      val members = cachedGroupMembers(groupId)
      Ok(views.html.group(group.id.toString, group.name, members, memberForm))
    }.getOrElse(
      NotFound("Oops, the group you're looking for does not exists :(")
    )
  }

  /**
   * Adds a new member to a group
   */
  def newGroupMember(groupId:String) = Action{ implicit request =>
    memberForm.bindFromRequest.fold(
      errors => cachedGroup(groupId).map{group =>
        BadRequest(views.html.group(groupId, group.name, cachedGroupMembers(groupId), errors))
      }.get,
      member => {
        UserDO.newGroupMember(UUID.fromString(groupId), UUID.fromString(member._1), member._2)
        Cache.set("groupMembers:"+groupId, null)
        Redirect(routes.Community.group(groupId))
      }
    )
  }

  /**
   * Gets a group from the cache if it is there.
   * Otherwise it gets it from the database and store it in the cache
   */
  private def cachedGroup(groupId:String):Option[Group] = {
    Cache.getOrElse("group:"+groupId, 0){
      UserDO.getGroupById(UUID.fromString(groupId))
    }
  }

  /**
   * Gets the users of a group from the cache.
   */
  private def cachedGroupMembers(groupId:String):List[User] = {
    Cache.getAs[List[User]]("groupMembers:"+groupId) match{
      case Some(mlst) if mlst != null => mlst
      case _ => val ulst = UserDO.getGroupMembers(UUID.fromString(groupId))
        Cache.set("groupMembers:"+groupId, ulst, 0)
        ulst
    }
  }

}
