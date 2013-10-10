package controllers

import project.dal.EzbProject
import models._
import utils.FormHelpers
import users.dal.Group
import project.dal._
import books.dal._
import forms.EzbForms

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
object Collaboration extends Controller with ContextProvider with FormHelpers {

  val memberMapping = mapping(
    "user_id" -> of[UUID],
    "assigned_part" -> text,
    "assigned_layer" -> of[UUID]
  )(TeamMember.apply)(TeamMember.unapply)

  def projectForm(ownerId:UUID, groupId:UUID) = Form[EzbProject](
    mapping(
      "project_id" -> of[UUID],
      "project_name" -> nonEmptyText,
      "project_owner" -> of[UUID],
      "project_creation" -> dateAsLong("dd-MM-yyyy"),
      "group_id" -> of[UUID],
      "new_ezb" -> default(boolean, true),
      "ezoombook_id" -> optional(of[UUID]),
      "project_team" -> list(memberMapping)
    )((pid, pname, powner, pcreation, groupid, newezb, ezbidop, pteam) =>
        EzbProject(pid, pname, powner, pcreation, groupid,
          if(newezb){UUID.randomUUID} else {ezbidop.get},
          pteam))
      ((proj:EzbProject) => Some((proj.projectId, proj.projectName, proj.projectOwnerId, proj.projectCreationDate,
        proj.groupId, false, Some(proj.ezoombookId), proj.projectTeam)))
  ).bind(
    Map(
      "project_id" -> UUID.randomUUID.toString,
      "project_owner" -> ownerId.toString,
      "project_creation" -> (new java.text.SimpleDateFormat("dd-MM-yyyy").format(new java.util.Date())), 
      "group_id" -> groupId.toString      
    )
  )

  val projectForm = Form[EzbProject](
    mapping(
      "project_id" -> of[UUID],
      "project_name" -> nonEmptyText,
      "project_owner" -> of[UUID],
      "project_creation" -> dateAsLong("dd-MM-yyyy"),
      "group_id" -> of[UUID],
      "new_ezb" -> default(boolean, true),
      "ezoombook_id" -> optional(of[UUID]),
      "project_team" -> list(memberMapping)
    )((pid, pname, powner, pcreation, groupid, newezb, ezbidop, pteam) =>
      EzbProject(pid, pname, powner, pcreation, groupid,
        if(newezb){UUID.randomUUID} else {ezbidop.get},
        pteam))
      ((proj:EzbProject) => Some((proj.projectId, proj.projectName, proj.projectOwnerId, proj.projectCreationDate,
        proj.groupId, false, Some(proj.ezoombookId), proj.projectTeam)))
  )

  /**
   * Displays the project edition form for a new project
   * @return
   */
//  def newPoject = Action {
//    implicit request =>
//      withUser { user =>
//          val emptyProject = EzbProject(UUID.randomUUID, "", user.id, (new java.util.Date()).getTime, UUID.randomUUID, UUID.randomUUID, List[TeamMember]())
//          Ok(views.html.projectedit(projectForm.fill(emptyProject), UserDO.userOwnedGroups(user.id)))
//      }
//  }

  /**
   * Displays the project edition form for an existing project
   */
  def editProject(projectId: String) = Action {
    implicit request =>
      context.user.map {
        user =>
          cachedProject(projectId).map {
            project =>
              Ok(views.html.projectedit(projectForm.fill(project), UserDO.userOwnedGroups(user.id)))
          }.getOrElse {
            Ok(views.html.projectedit(projectForm, UserDO.userOwnedGroups(user.id)))
          }
      }.getOrElse {
        Unauthorized("Oops! you need to be connected to access this page")
      }
  }

  /**
   * Stores the created/edited project in the database
   * @return
   */
  def saveProject = Action {
    implicit request =>
      withUser {
        user =>
          projectForm.bindFromRequest.fold(
            errors => {
	      println("[ERROR] Could not create project from form: " + errors.errors.map(err => err.key + " " + err.message))
              BadRequest(views.html.projectedit(errors, UserDO.userOwnedGroups(user.id)))
            },
            ezbProject => {
              BookDO.saveProject(ezbProject)	     
              if (projectForm.bindFromRequest.data.getOrElse("new_ezb", "false").toBoolean){
		val ezbform = EzbForms.ezoomBookForm.bind(Map(
		    "ezb_id" -> ezbProject.ezoombookId.toString,
		    "ezb_owner" -> user.id.toString,
		    "ezb_status" -> books.dal.Status.workInProgress.toString,
		    "ezb_title" -> "",
		    "ezb_public" -> "false"
		  ))

                Ok(views.html.ezbbooklist(BookDO.listBooks, ezbform, ezbProject.projectId.toString))
	      }else{
		Redirect(routes.Collaboration.projectAdmin(ezbProject.projectId.toString))
	      }
            }
          )
      }
  }

  def saveProjectEzb(projectId:String) = Action{ implicit request =>
    withUser{ user =>
      EzbForms.ezoomBookForm.bindFromRequest.fold(
	errors => {
	  println("[ERROR] Could not create eZoomBook for project " + projectId)
	  Redirect(routes.Collaboration.projectAdmin(projectId))
	},
	ezb => {
	  BookDO.saveEzoomBook(ezb)
	  Redirect(routes.Collaboration.projectAdmin(projectId))
	}
      )
    }
  }

  /**
   * Displays the project administration page
   * @param projId
   * @return
   */
  def projectAdmin(projId: String) = Action {
    implicit request =>
      withUser { user =>
	  BookDO.getProject(UUID.fromString(projId)).map{project =>	    
	    Ok(views.html.ezbproject(project,
                project.projectTeam.flatMap(m => UserDO.getUser(m.userId)),
                BookDO.getEzoomBook(project.ezoombookId),
                Form(memberMapping)))
	  }.getOrElse{
	    println("[ERROR] Project " + projId + " not found")
	    Redirect(routes.Application.home)
	  }
      }
  }

  def newProjectMember(projId: String) = Action {
    implicit request =>
      val pId = UUID.fromString(projId)
      withUser {
        user =>
          cachedProject(projId).map {
            ezbProject =>
              Form(memberMapping).bindFromRequest.fold(
                err => {
                  println("[ERROR] Found errors on form Form(memberMapping): " + err)
                  BadRequest(views.html.ezbproject(ezbProject,
                    ezbProject.projectTeam.flatMap(m => UserDO.getUser(m.userId)),
                    BookDO.getEzoomBook(ezbProject.ezoombookId),
                    err))
                },
                member => {
                  BookDO.addProjectMember(pId, member).map {
                    newProj =>
                      Redirect(routes.Collaboration.projectAdmin(projId))
                  }.getOrElse {
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

  def deleteProject(projId:String) = Action { implicit request =>
    withUser{ user =>
      BookDO.deleteProject(UUID.fromString(projId))
      Redirect(routes.Application.home)
    }
  }

  /**
   * Gets a group from the cache if it is there.
   * Otherwise it gets it from the database and store it in the cache
   */
  private def cachedProject(projId: String): Option[EzbProject] = {
    Cache.getOrElse("project:" + projId, 0) {
      BookDO.getProject(UUID.fromString(projId))
    }
  }

}
