package controllers

import models._
import users.dal._
import books.dal._
import project.dal._

import play.api._
import users.dal.Group
import users.dal.User
import scala.Some
import cache.Cache

import Play.current

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.i18n.Messages
import Forms._
import java.util.UUID
import jp.t2v.lab.play2.auth.AuthElement

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
object Community extends Controller with AuthElement with AuthConfigImpl  with ContextProvider{

  val memberForm = Form(
    tuple(
      "member_email" -> text,
      "role" -> text
    )
  )
  
  val groupForm = Form(
    tuple(
      "groupName" -> text,
      "ownerId" -> text
    )
  
    )


  /**
   * Displays the deails of a group
   */
  def group(gid:String) = StackAction(AuthorityKey -> RegisteredUser){ implicit request =>
    val groupId = UUID.fromString(gid)
    withUser{user =>
      cachedGroup(gid).map{group =>
        val members = UserDO.getGroupMembers(groupId)
        Ok(views.html.group(group, members,
          BookDO.getGroupProjects(groupId),
          memberForm,
          BookDO.getUserEzoombooks(user.id),
          Collaboration.projectFrm(user.id, groupId)))
      }.getOrElse(
        NotFound("Oops, the group you're looking for does not exists :(")
      )
    }
  }

  /**
   * Adds a new member to a group
   */
  def newGroupMember(gid:String) = StackAction(AuthorityKey -> RegisteredUser){ implicit request =>
    val groupId = UUID.fromString(gid)
    withUser{ user =>
      memberForm.bindFromRequest.fold(
        errors => cachedGroup(gid).map{group =>
          println("[ERROR] Errors found while trying to create group member : " + errors)
          BadRequest(views.html.group(group,
            UserDO.getGroupMembers(groupId),
            BookDO.getGroupProjects(groupId),
            errors,
            BookDO.getUserEzoombooks(user.id),
            Collaboration.projectForm.fill(Collaboration.emptyProject(user.id,groupId))))
        }.get,
        member => {
          UserDO.getUserId(member._1).map{userId =>
            UserDO.newGroupMember(groupId, userId, member._2)
            Redirect(routes.Community.group(gid))
          }.getOrElse{
            cachedGroup(gid).map{group =>
              BadRequest(views.html.group(group,
                UserDO.getGroupMembers(groupId),
                BookDO.getGroupProjects(groupId),
                memberForm.withGlobalError(Messages("group.memberfrom.error.usernotfound",member._1)),
                BookDO.getUserEzoombooks(user.id),
                Collaboration.projectForm))
            }.get
          }
        }
      )
    }
  }

  /**
   * Creates a new group
    * @return
   */
  def newGroup = StackAction(AuthorityKey -> RegisteredUser){ implicit request =>
      withUser{ user =>
        Form[String]("groupName" -> text).bindFromRequest.fold(
          err => {
            println("[ERRO] Could not create group. Errors in form: " + err)
            Redirect(routes.Workspace.home)
          },
          groupName => {
            UserDO.newGroup(groupName, user.id)
            Redirect(routes.Workspace.home)
          }
        )
      }
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
  private def cachedGroupMembers(groupId:String):List[(User,AppDB.dal.Roles.Value)] = {
    Cache.getAs[List[(User,AppDB.dal.Roles.Value)]]("groupMembers:"+groupId) match{
      case Some(mlst) if mlst != null => mlst
      case _ => val ulst = UserDO.getGroupMembers(UUID.fromString(groupId))
        Cache.set("groupMembers:"+groupId, ulst, 0)
        ulst
    }
  }


  def groupadmin(groupId:String) = StackAction(AuthorityKey -> RegisteredUser){ implicit request =>
    withUser{user =>
      cachedGroup(groupId).map{group =>
        val members = cachedGroupMembers(groupId)
        Ok(views.html.groupadmin(group.id.toString, group.name, members, memberForm))
      }.getOrElse(
        NotFound("Oops, the group you're looking for does not exists :(")
      )
    }
  }
}
