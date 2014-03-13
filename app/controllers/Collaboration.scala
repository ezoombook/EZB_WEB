package controllers

import project.dal.EzbProject
import models._
import utils.FormHelpers
import users.dal._
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
import jp.t2v.lab.play2.auth.AuthElement
import models.ListedProject

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try}
import ExecutionContext.Implicits.global

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 12/06/13
 * Time: 20:08
 * To change this template use File | Settings | File Templates.
 */
object Collaboration extends Controller with AuthElement with AuthConfigImpl with ContextProvider with FormHelpers {

  val memberMapping = mapping(
    "user_id" -> of[UUID],
    "assigned_parts" -> list(text),
    "assigned_layer" -> text,
    "has_all_parts" -> default(boolean, true)
  )(TeamMember.apply)(TeamMember.unapply)

  val projectForm = Form[EzbProject](
    mapping(
      "project_id" -> of[UUID],
      "project_name" -> nonEmptyText,
      "project_owner" -> of[UUID],
      "project_creation" -> dateAsLong("dd-MM-yyyy"),
      "group_id" -> of[UUID],
      "new_ezb" -> default(boolean, true),
      "multi_level" -> default(boolean, false),
      "level" -> optional(number),
      "ezoombook_id" -> optional(of[UUID]),
      "project_team" -> list(memberMapping)
    )((pid, pname, powner, pcreation, groupid, newezb, multilev, level, ezbidop, pteam) =>
      EzbProject(pid, pname, powner, pcreation, groupid, ezbidop, multilev, level, pteam))
      ((proj: EzbProject) => Some((proj.projectId, proj.projectName, proj.projectOwnerId, proj.projectCreationDate,
        proj.groupId, false, proj.isMultiLevel, proj.level, proj.ezoombookId, proj.projectTeam)))
  )

  /**
   * Returns a pre-filled form for a new project
   * @param ownerId owner of the project
   * @param groupId group to which this project belongs
   */

  def projectFrm(ownerId: UUID, groupId: UUID) = projectForm.bind(
    Map(
      "project_id" -> UUID.randomUUID.toString,
      "project_owner" -> ownerId.toString,
      "project_creation" -> (new java.text.SimpleDateFormat("dd-MM-yyyy").format(new java.util.Date())),
      "group_id" -> groupId.toString,
      "new_ezb" -> "true",
      "multi_level" -> "false",
      "level" -> Some(1).toString
    )
  )

  /**
   * Creates an empty project pre-filled  form
   * @return
   */
   def emptyProject(ownerId:UUID,groupId:UUID) =
      EzbProject(UUID.randomUUID, "", ownerId, (new java.util.Date()).getTime,
        groupId, None, false, None, List[TeamMember]())

  /**
   * Stores the created/edited project in the database
   * @return
   */
  def saveProject(groupId:String) = StackAction(AuthorityKey -> RegisteredUser) {
    implicit request =>
      withUser {
        user =>
          projectForm.bindFromRequest.fold(
            errors => {
              Logger.debug("[ERROR] Could not create project from form: " + errors.errors.map(err => err.key + " " + err.message))
              Redirect(routes.Workspace.home)
            },
            ezbProject => {
              BookDO.saveProject(ezbProject)
              ezbProject.ezoombookId.map{ezbid =>
                //Change eZoomBook owner and create / change layer owner
                BookDO.getEzoomBook(ezbid).map{ezb =>
                  if (!ezbProject.isMultiLevel && !ezbProject.level.isEmpty){
                    ezb.ezoombook_layers.get(ezbProject.level.get.toString) match{
                      case Some(layerId) =>
                        BookDO.changeLayerOwner(UUID.fromString(layerId), "group:" + groupId)
                      case _ => val newLayer = EzoomLayer(UUID.randomUUID, ezb.ezoombook_id, ezbProject.level.get,
                        "group:"+groupId, books.dal.Status.workInProgress, false)
                        BookDO.saveLayer(newLayer)
                    }
                  }
                  val newEzb = ezb.copy(ezoombook_owner = "group:"+groupId)
                  BookDO.saveEzoomBook(newEzb)
                }
                Redirect(routes.Collaboration.projectAdmin(ezbProject.projectId.toString))
              }.getOrElse{
                Redirect(routes.Collaboration.ezbProjectBookList("group:"+groupId,
                  ezbProject.projectId.toString))
              }
            }
          )
      }
  }

  /**
   * Display a list of books for creating a new eZoomBook for a project
   * @param owner Owner of the EZB: normally, the group
   * @param projectId Id of the project
   */
  def ezbProjectBookList(owner:String, projectId:String) = StackAction(AuthorityKey -> RegisteredUser){implicit request =>
    withUser{user =>
      val ezbform = EzbForms.ezoomBookForm.bind(Map(
        "ezb_id" -> UUID.randomUUID().toString,
        "ezb_project" -> projectId,
        "ezb_owner" -> owner,
        "ezb_status" -> books.dal.Status.workInProgress.toString,
        "ezb_title" -> "",
        "ezb_public" -> "false"
      ))

      Ok(views.html.ezbbooklist(BookDO.listBooks, ezbform, projectId))
    }
  }

  /**
   * Creates a new eZoomBook for a project based on a form
   * and updates the project
   * @param projectId Id of the project
   */
  def saveProjectEzb(projectId: String) = AsyncStack(AuthorityKey -> RegisteredUser) {
    implicit request =>
      withUserAsync {
        user =>
          EzbForms.ezoomBookForm.bindFromRequest.fold(
            errors => {
              Logger.error("Could not create eZoomBook for project " + projectId +
                errors.errors.map(err => err.key + " -> " + err.message).mkString("\n"))
              Future.successful(Redirect(routes.Collaboration.projectAdmin(projectId)))
            },
            ezb => {
              BookDO.saveEzoomBook(ezb)
              BookDO.updateProjectEzb(UUID.fromString(projectId), ezb.ezoombook_id).map{projtry =>
                projtry.map{
                  proj =>
                    Logger.debug(s"Project $proj successfully updated!")
                    Redirect(routes.Collaboration.projectAdmin(projectId))
                }.recover{
                  case err =>
                  Logger.error(s"Could not update project $projectId: " + err)
                  Redirect(routes.Collaboration.projectAdmin(projectId))
                }.get
              }
            }
          )
      }
  }

  private def canEditProject(project:EzbProject)(user:User):Boolean = {
    UserDO.getGroupById(project.groupId).exists(_.ownerId == user.id) ||
    UserDO.getGroupMembers(project.groupId).exists{
      case (usr, role) => usr.id == user.id && role == AppDB.dal.Roles.coordinator
    }
  }

  /**
   * Displays the project administration page
   * @param projId
   * @return
   */
  def projectAdmin(projId: String) = AsyncStack(AuthorityKey -> RegisteredUser) {
    implicit request =>
      withUserAsync {
        user =>
          BookDO.getProject(UUID.fromString(projId)).map {opt =>
            opt.map{ project =>
              val ezbopt = project.ezoombookId.flatMap(BookDO.getEzoomBook(_))
              val bookParts = ezbopt.flatMap(ezb => BookDO.getBook(ezb.book_id.toString)).map{book =>
                book.bookParts
              }.getOrElse(List[BookPart]())

              Ok(views.html.ezbproject(project,
                project.projectTeam.flatMap(m => UserDO.getUser(m.userId)),
                ezbopt,
                Form(memberMapping),
                ezbopt.map(ezb => BookDO.getCommetsByEzb(ezb.ezoombook_id)).getOrElse(List[Comment]()),
                canEditProject(project),
                bookParts,
                if(project.ezoombookId.isEmpty){BookDO.getUserEzoombooks(user.id)} else {List[Ezoombook]()}))
            }.getOrElse {
              println("[ERROR] Project " + projId + " not found")
              Redirect(routes.Workspace.home)
            }
          }
      }
  }

  /**
   * Sets the eZoomBook the related to a project
   * @param projId The id of the project that is modified.
   *               Note that this action has no effect on a project with an already defined eZoomBook.
   */
  def setProjectEzb(projId: String) = AsyncStack(AuthorityKey -> RegisteredUser) {
    implicit request =>
      withUserAsync {
        user =>
          BookDO.getProject(UUID.fromString(projId)).map {opt =>
            opt.map{
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
  }

  /**
   * Adds a new member to the project team,
   * while assigning him a layer and/or a part to work on.
   * @param projId
   * @return
   */
  def newProjectMember(projId: String) = StackAction(AuthorityKey -> RegisteredUser) {
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

  def editProjectMember(projId:String) = StackAction(AuthorityKey -> RegisteredUser) {implicit request =>
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

  def removeProjMember(projId:String, memId:String) = AsyncStack(AuthorityKey -> RegisteredUser){
    implicit request =>
      for{
        pid <- projId.toUUID.fold(err => Future.failed(new Exception(err)), ok => Future.successful(ok))
        mem <- memId.toUUID.fold(err => Future.failed(new Exception(err)), ok => Future.successful(ok))
        _ <- BookDO.deleteProjectMember(pid,mem)
      }yield(
        Redirect(routes.Collaboration.projectAdmin(projId))
       )
  }

  def deleteProject(projId: String) = AsyncStack(AuthorityKey -> RegisteredUser) {
    implicit request =>
      withUserAsync{user =>
        for{
          pid <- projId.toUUID.fold(err => Future.failed(new Exception(err)), ok => Future.successful(ok))
          olds <- getProjectsByUser(user.id)
          _ <- BookDO.deleteProject(UUID.fromString(projId))
        } yield(
          Ok(views.html.workspace(
            olds.filterNot(_.projId.toString == projId),
            BookDO.getUserEzoombooks(user.id),
            BookDO.getUserBooks(user.id),
            UserDO.userOwnedGroups(user.id),
            UserDO.userIsMemberGroups(user.id), Workspace.groupForm))
        )
      }
  }

  def saveComment = StackAction(AuthorityKey -> RegisteredUser) {implicit request =>
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
   * Returns the list of projects the user owns or participates into
   */
  def getProjectsByUser(userId:UUID):Future[List[ListedProject]] = {
      val projSetFuture = for {
        owned <- BookDO.getOwnedProjects(userId)
        others <- BookDO.getProjectsByMember(userId)
      } yield (owned.toSet ++ others.toSet)


      projSetFuture.map{projSet =>
        projSet.map{
          proj =>
            proj.ezoombookId.flatMap(BookDO.getEzoomBook(_)).map {
              ezb =>
                ListedProject(proj.projectId, proj.projectName, proj.projectOwnerId,
                  proj.projectCreationDate, Some(ezb.ezoombook_id), ezb.ezoombook_title)
            }.getOrElse {
              ListedProject(proj.projectId, proj.projectName, proj.projectOwnerId,
                proj.projectCreationDate, None, "")
            }
        }.toList
      }
  }

  /**
   * Gets a group from the cache if it is there.
   * Otherwise it gets it from the database and store it in the cache
   */
//  private def cachedProject(projId: String): Option[EzbProject] = {
//    Cache.getOrElse("project:" + projId, 0) {
//      BookDO.getProject(UUID.fromString(projId))
//    }
//  }

}
