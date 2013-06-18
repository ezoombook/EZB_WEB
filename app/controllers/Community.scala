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
import project.dal.{TeamMember, EzbProject}


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
object Community extends Controller with ContextProvider{

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
   * Creates an empty project for pre-filling the project form
   * @return
   */
  private def emptyProject(ownerId:UUID,groupId:UUID) =
    EzbProject(UUID.randomUUID, "", ownerId, (new java.util.Date()).getTime,
      groupId, UUID.randomUUID, List[TeamMember]())

  /**
   * Displays the groups owned by a user
   */
  def groups = Action{ implicit request =>
    session.get("userId").map(UUID.fromString(_)).map{uid =>
      Ok(views.html.workspace(UserDO.userOwnedGroups(uid), UserDO.userIsMemberGroups(uid),groupForm))
    }.getOrElse(
      Unauthorized("Oops, you are not connected")
    )
  }

  /**
   * Displays the deails of a group
   */
  def group(gid:String) = Action{ implicit request =>
    val groupId = UUID.fromString(gid)
    context.user.map{user =>
      cachedGroup(gid).map{group =>
        val members = cachedGroupMembers(gid)
        Ok(views.html.group(group.id.toString, group.name, members,
          BookDO.getGroupProjects(groupId),
          memberForm, Collaboration.projectForm.fill(emptyProject(user.id,groupId))))
      }.getOrElse(
        NotFound("Oops, the group you're looking for does not exists :(")
      )
    }.getOrElse{
      Unauthorized("Oops, you are not connected")
    }
  }

  /**
   * Adds a new member to a group
   */
  def newGroupMember(gid:String) = Action{ implicit request =>
    val groupId = UUID.fromString(gid)
    withUser{ user =>
      memberForm.bindFromRequest.fold(
        errors => cachedGroup(gid).map{group =>
          println("[ERROR] Errors found while trying to create group member : " + errors)
          BadRequest(views.html.group(gid, group.name, cachedGroupMembers(gid),
            BookDO.getGroupProjects(groupId),
            errors, Collaboration.projectForm.fill(emptyProject(user.id,groupId))))
        }.get,
        member => {
          UserDO.getUserId(member._1).map{userId =>
            UserDO.newGroupMember(groupId, userId, member._2)
            Cache.set("groupMembers:"+groupId, null)
            Redirect(routes.Community.group(gid))
          }.getOrElse{
            cachedGroup(gid).map{group =>
              val members = cachedGroupMembers(gid)
              BadRequest(views.html.group(gid, group.name, members,
                BookDO.getGroupProjects(groupId),
                memberForm.withGlobalError("Cannot find the user with mail " + member._1), Collaboration.projectForm))
            }.get
          }
        }
      )
    }
  }
  
 
    //((name, ownerId)=>Group(UUID.randomUUID, name, ownerId))
    //((group:Group)=>(group.name, group.ownerId))
   def newGroup = Action{ implicit request =>
    session.get("userId").map(UUID.fromString(_)).map{uid =>
      groupForm.bindFromRequest.fold(
      errors => 
        BadRequest(views.html.workspace(UserDO.userOwnedGroups(uid), UserDO.userIsMemberGroups(uid),groupForm))
      ,
      (group)=>{UserDO.newGroup(group._1, uid)
      Ok(views.html.workspace(UserDO.userOwnedGroups(uid), UserDO.userIsMemberGroups(uid),groupForm))
      }
    )}
    .getOrElse(
      Unauthorized("Oops, you are not connected")
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
  private def cachedGroupMembers(groupId:String):List[(User,AppDB.dal.Roles.Value)] = {
    Cache.getAs[List[(User,AppDB.dal.Roles.Value)]]("groupMembers:"+groupId) match{
      case Some(mlst) if mlst != null => mlst
      case _ => val ulst = UserDO.getGroupMembers(UUID.fromString(groupId))
        Cache.set("groupMembers:"+groupId, ulst, 0)
        ulst
    }
  }


  def groupadmin(groupId:String) = Action{ implicit request =>
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
