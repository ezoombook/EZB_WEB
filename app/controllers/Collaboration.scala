package controllers

import project.dal.EzbProject
import models._
import utils.FormHelpers
import users.dal.Group
import project.dal._
import books.dal._

import play.api._
import play.api.mvc._
import play.api.data._
import Forms._
import Play.current
import java.util.UUID
import play.api.cache.Cache

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 12/06/13
 * Time: 20:08
 * To change this template use File | Settings | File Templates.
 */
object Collaboration extends Controller with ContextProvider with FormHelpers{

  val memberMapping = mapping(
    "user_id" -> of[UUID],
    "assigned_part" -> text,
    "assigned_layer" -> of[UUID]
  )(TeamMember.apply)(TeamMember.unapply)

  val projectForm = Form(
    mapping(
      "project_id" -> of[UUID],
      "project_name" -> nonEmptyText,
      "project_owner" -> of[UUID],
      "project_creation" -> dateAsLong("dd-MM-yyyy"),
      "group_id" -> of[UUID],
      "ezoombook_id" -> of[UUID],
      "project_team" -> list(memberMapping)
    )(EzbProject.apply)(EzbProject.unapply)
  )

  /**
   * Displays the project edition form for a new project
   * @return
   */
  def newPoject = Action{implicit request =>
    context.user.map{user =>
      val emptyProject = EzbProject(UUID.randomUUID, "", user.id, (new java.util.Date()).getTime, UUID.randomUUID, UUID.randomUUID, List[TeamMember]())
      Ok(views.html.projectedit(projectForm.fill(emptyProject), UserDO.userOwnedGroups(user.id)))
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
        Ok(views.html.projectedit(projectForm.fill(project), UserDO.userOwnedGroups(user.id)))
      }.getOrElse{
        Ok(views.html.projectedit(projectForm, UserDO.userOwnedGroups(user.id)))
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
    context.user.map{ user =>
      projectForm.bindFromRequest.fold(
        errors => {
          BadRequest(views.html.projectedit(errors, UserDO.userOwnedGroups(user.id)))
        },
        ezbProject =>{
          BookDO.saveProject(ezbProject)
          Cache.set("project:"+ezbProject.projectId, ezbProject)
          Ok(views.html.ezbproject(ezbProject,
            ezbProject.projectTeam.flatMap(m => UserDO.getUser(m.userId)),
            BookDO.getEzoomBook(ezbProject.ezoombookId),
            Form(memberMapping)))
        }
      )
    }.getOrElse{
      Unauthorized("Oops! you need to be connected to access this page")
    }
  }

  /**
   * Displays the project administration page
   * @param projId
   * @return
   */
  def projectAdmin(projId:String) = Action{implicit request =>
    withUser{user =>
      cachedProject(projId).map{project =>
        println("[INFO] project: " + project.projectTeam.flatMap(m => UserDO.getUser(m.userId)))
        Ok(views.html.ezbproject(project,
          project.projectTeam.flatMap(m => UserDO.getUser(m.userId)),
          BookDO.getEzoomBook(project.ezoombookId),
          Form(memberMapping)))
      }.getOrElse{
        val listproj = BookDO.getOwnedProjects(user.id).foldLeft(List[(EzbProject,Ezoombook)]()){(list,proj) =>
       BookDO.getEzoomBook(proj.ezoombookId).map{ezb =>
       list :+ (proj,ezb) 
     }.getOrElse{list}}
        BadRequest(views.html.workspace(listproj, UserDO.userOwnedGroups(user.id),
          UserDO.userIsMemberGroups(user.id),Community.groupForm))
      }
    }
  }

  def newProjectMember(projId:String) = Action{implicit request =>
    val pId = UUID.fromString(projId)
    withUser{user =>
      cachedProject(projId).map{ ezbProject =>
        Form(memberMapping).bindFromRequest.fold(
          err =>{
            println("[ERROR] Found errors on form Form(memberMapping): " + err)
            BadRequest(views.html.ezbproject(ezbProject,
              ezbProject.projectTeam.flatMap(m => UserDO.getUser(m.userId)),
              BookDO.getEzoomBook(ezbProject.ezoombookId),
              err))
          },
          member => {
            BookDO.addProjectMember(pId, member).map{newProj =>
              Redirect(routes.Collaboration.projectAdmin(projId))
            }.getOrElse{
              BadRequest(views.html.ezbproject(ezbProject,
                ezbProject.projectTeam.flatMap(m => UserDO.getUser(m.userId)),
                BookDO.getEzoomBook(ezbProject.ezoombookId),
                Form(memberMapping).withGlobalError("Could not add member")))
            }
          }
        )
      }.get
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
