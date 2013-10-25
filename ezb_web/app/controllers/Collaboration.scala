package controllers

import project.dal.EzbProject
import models._
import utils.FormHelpers
import users.dal.Group
import project.dal._
import books.dal._
import ezb.comments._
import forms.{EzbForms, AppForms}

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
    "assigned_layer" -> text
  )(TeamMember.apply)(TeamMember.unapply)

  def projectForm(ownerId: UUID, groupId: UUID) = Form[EzbProject](
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
      EzbProject(pid, pname, powner, pcreation, groupid, ezbidop, pteam))
      ((proj: EzbProject) => Some((proj.projectId, proj.projectName, proj.projectOwnerId, proj.projectCreationDate,
        proj.groupId, false, proj.ezoombookId, proj.projectTeam)))
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
      EzbProject(pid, pname, powner, pcreation, groupid, ezbidop, pteam))
      ((proj: EzbProject) => Some((proj.projectId, proj.projectName, proj.projectOwnerId, proj.projectCreationDate,
        proj.groupId, false, proj.ezoombookId, proj.projectTeam)))
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
              if (projectForm.bindFromRequest.data.getOrElse("new_ezb", "false").toBoolean) {
                val ezbform = EzbForms.ezoomBookForm.bind(Map(
                  "ezb_id" -> ezbProject.ezoombookId.toString,
                  "ezb_owner" -> user.id.toString,
                  "ezb_status" -> books.dal.Status.workInProgress.toString,
                  "ezb_title" -> "",
                  "ezb_public" -> "false"
                ))

                Ok(views.html.ezbbooklist(BookDO.listBooks, ezbform, ezbProject.projectId.toString))
              } else {
                Redirect(routes.Collaboration.projectAdmin(ezbProject.projectId.toString))
              }
            }
          )
      }
  }

  def saveProjectEzb(projectId: String) = Action {
    implicit request =>
      withUser {
        user =>
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
      withUser {
        user =>
          BookDO.getProject(UUID.fromString(projId)).map {
            project =>
              val ezbopt = project.ezoombookId.flatMap(BookDO.getEzoomBook(_))
              val bookParts = ezbopt.flatMap(ezb => BookDO.getBook(ezb.book_id.toString)).map{book =>
                book.bookParts
              }.getOrElse(List[BookPart]())

              Ok(views.html.ezbproject(project,
                project.projectTeam.flatMap(m => UserDO.getUser(m.userId)),
                ezbopt,
                Form(memberMapping),
                ezbopt.map(ezb => BookDO.getCommetsByEzb(ezb.ezoombook_id)).getOrElse(List[Comment]()),
                bookParts,
                if(project.ezoombookId.isEmpty){BookDO.getUserEzoombooks(user.id)} else {List[Ezoombook]()}))
          }.getOrElse {
            println("[ERROR] Project " + projId + " not found")
            Redirect(routes.Application.home)
          }
      }
  }

  /**
   * Sets the eZoomBook the related to a project
   * @param projId The id of the project that is modified.
   *               Note that this action has no effect on a project with an already defined eZoomBook.
   */
  def setProjectEzb(projId: String) = Action {
    implicit request =>
      withUser {
        user =>
          BookDO.getProject(UUID.fromString(projId)).map {
            case project if (project.ezoombookId.isEmpty) =>
              Form("ezbId" -> of[UUID]).bindFromRequest.fold(
                errors => {
                  println("[ERROR] An error occurred while trying to save project's ezb: " + errors)
                  Redirect(routes.Collaboration.projectAdmin(projId))
                },
                ezbId => {
                  BookDO.saveProject(project.copy(ezoombookId = Some(ezbId)))
                  Redirect(routes.Collaboration.projectAdmin(projId))
                }
              )
          }.getOrElse {
            Redirect(routes.Collaboration.projectAdmin(projId))
          }
      }
  }

  def newProjectMember(projId: String) = Action {
    implicit request =>
      val pId = UUID.fromString(projId)
      withUser {
        user =>
          Form(memberMapping).bindFromRequest.fold(
            err => {
              println("[ERROR] Found errors on form Form(memberMapping): " + err)
              Redirect(routes.Collaboration.projectAdmin(projId))
            },
            member => {
              BookDO.addProjectMember(pId, member)
              Redirect(routes.Collaboration.projectAdmin(projId))
            }
          )
      }
  }

  def editProjectMember(projId:String) = Action {implicit request =>
    val pid=UUID.fromString(projId)
    withUser{ user =>
      Form(memberMapping).bindFromRequest.fold(
        err => {
          println("[ERROR] Found errors on form Form(memberMapping): " + err)
          Redirect(routes.Collaboration.projectAdmin(projId))
        },
        member => {
          BookDO.updateProjectMember(pid, member)
          Redirect(routes.Collaboration.projectAdmin(projId))
        }
      )
    }
  }

  def deleteProject(projId: String) = Action {
    implicit request =>
      withUser {
        user =>
          BookDO.deleteProject(UUID.fromString(projId))
          Redirect(routes.Application.home)
      }
  }

  def saveComment = Action{implicit request =>
    val referer = request.headers.get(REFERER).getOrElse(Application.HOME_URL)
    AppForms.commentForm.bindFromRequest.fold(
      err => {
        println("[ERR] An error occurred while sending the comment: " + err.errors.map(e => e.key + " : " + e.message).mkString("\n"))
        Redirect(referer)
      },
      comment => {
        BookDO.saveComment(comment)
        Redirect(referer)
      }
    )
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
