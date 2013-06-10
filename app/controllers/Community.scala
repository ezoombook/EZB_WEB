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
object Community extends Controller with ContextProvider{

  val memberForm = Form(
    tuple(
      "member id" -> text,
      "role" -> text
    )
  )
  
  val groupForm = Form(
    tuple(
      "groupName" -> text,
      "ownerId" -> text
    )
  
    )
   
      val projectForm = Form(
    tuple(
      "projectName" -> text,
      "ownerId" -> text
    )
  
    )
    

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
  def group(groupId:String) = Action{ implicit request =>
    cachedGroup(groupId).map{group =>
      val members = cachedGroupMembers(groupId)
      Ok(views.html.group(group.id.toString, group.name, members, memberForm))
    }.getOrElse(
      NotFound("Oops, the group you're looking for does not exists :(")
    )
  }

    /**
   * Displays the deails of a project
   */
 def project/*(projectId:String)*/ = Action{ implicit request =>
   
    //cachedProject(projectId).map{project =>
      //val members = cachedProjectMembers(projectId)
      //project.id.toString, project.name, members, memberForm
      Ok(views.html.project())
    //}.getOrElse(
      //NotFound("Oops, the project you're looking for does not exists :(")
    //)
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
  
  
  //def newProject = Action{ implicit request =>
    //session.get("userId").map(UUID.fromString(_)).map{uid =>
      //projectForm.bindFromRequest.fold(
      //errors => 
        //BadRequest(views.html.group(group.id.toString, group.name, members, memberForm, projectForm))
      //,
      //(project)=>{UserDO.newProject(project._1, uid)
      //Ok(views.html.workspace(group.id.toString, group.name, members, memberForm, projectForm))
      //}
    //)}
    //.getOrElse(
      //Unauthorized("Oops, you are not connected")
    //)
  //}
    
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


  def groupadmin(groupId:String) = Action{ implicit request =>
    cachedGroup(groupId).map{group =>
      val members = cachedGroupMembers(groupId)
      Ok(views.html.groupadmin(group.id.toString, group.name, members, memberForm))
    }.getOrElse(
      NotFound("Oops, the group you're looking for does not exists :(")
    )
  }
}
