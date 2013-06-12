package controllers

import project.dal.EzbProject
import models._
import utils.FormHelpers

import play.api._
import play.api.mvc._
import play.api.data._
import Forms._
import Play.current
import java.util.UUID
import users.dal.Group
import play.api.cache.Cache

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 12/06/13
 * Time: 20:08
 * To change this template use File | Settings | File Templates.
 */
object Collaboration extends Controller with ContextProvider with FormHelpers{

  val projectForm = Form(
    mapping(
      "project_id" -> of[UUID],
      "project_name" -> nonEmptyText,
      "project_owner" -> of[UUID],
      "project_creation" -> of[Long],
      "group_id" -> of[UUID],
      "ezoombook_id" -> of[UUID],
      "project_team" -> list(memberMapping)
    )(EzbProject.apply)(EzbProject.unapply)
  )

  val memberMapping = mapping(
    "user_id" -> of[UUID],
    "assigned_part" -> text,
    "assigned_layer" -> of[UUID]
  )(TeamMember.apply)(TeamMember.unapply)

  /**
   * Displays the project edition form for a new project
   * @return
   */
  def newPoject = Action{implicit request =>
    context.user.map{user =>
      val emptyProject = Project(UUID.randomUUID, "", user.id, new java.util.Date(), UUID.randomUUID, UUID.randomUUID, List[TeamMember]())
      Ok(views.html.projectedit(projectForm.fill(emptyProject)))
    }.getOrElse{
      Unauthorized("Oops! you need to be connected to access this page")
    }
  }

  /**
   * Displays the project edition form for an existing project
   */
  def editProject(projectId:String) = Action{implicit request =>
    context.user.map{user =>
      cachedProject(projectId).map{project =>
        Ok(views.html.projectedit(projectForm.fill(project)))
      }.getOrElse{
        Ok(views.html.projectedit(projectForm))
      }
    }.getOrElse{
      Unauthorized("Oops! you need to be connected to access this page")
    }
  }

  /**
   * Stores the created/edited project in the database
   * @return
   */
  def saveProject = Action{implicit request =>
    projectForm.bindFromRequest.fold(
      errors => {
        BadRequest(views.html.projectedit(errors))
      },
      ezbProject =>{
        BookDO.saveProject(ezbProject)
        cache.set("project:"+ezbProject.projectId, projectId)
        Ok(views.html.projectadmin(ezbProject))
      }
    )
  }

  /**
   * Displays the project administration page
   * @param projId
   * @return
   */
  def projectAdmin(projId:String) = Action{implicit request =>
    context.user.map{
      cachedProject(projId).map{project =>
        Ok(views.html.projectadmin(project))
      }.getOrElse{
        Redirect(routs.Collaboration.newProject)
      }
    }.getOrElse{
      Unauthorized("Oops! you need to be connected to access this page")
    }
  }

  /**
   * Gets a group from the cache if it is there.
   * Otherwise it gets it from the database and store it in the cache
   */
  private def cachedProject(projId:String):Option[EzbProject] = {
    Cache.getOrElse("project:"+projId, 0){
      BookDO.getProject(UUID.fromString(projId))
    }
  }

}
