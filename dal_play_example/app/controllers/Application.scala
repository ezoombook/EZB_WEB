package controllers

import models._
import users.dal._
import books.dal._
import EzbForms._

import play.api._
import play.api.mvc._
import play.api.data._
import Forms._
import cache.Cache

import Play.current

import java.io.ByteArrayInputStream
import java.util.UUID
import play.api.libs.json.Json

object Application extends Controller {
  
  def index = Action {
    Redirect(routes.Application.users)
  }
  
  def users = Action {
    Ok(views.html.index(UserDO.listUsers, userForm))
  }

  def newUser = Action { implicit request =>
    userForm.bindFromRequest.fold(
      errors => {
	BadRequest(views.html.index(UserDO.listUsers, errors))
      },
      user => {
	UserDO.create(user)
	Redirect(routes.Application.users)
      }
    )
  }

  def validate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      errors => {
	BadRequest(views.html.login(errors))
      },
      up => {
	if (UserDO.validateUser(up._1, up._2))
	  UserDO.getUser(up._1).map(uid =>
	    Redirect(routes.Application.home).withSession(
	      "userId" -> uid.toString,
	      "userName" -> up._1
	    )
	  ).getOrElse(
	    BadRequest(views.html.login(loginForm))
	  )
	else	       
	  BadRequest(views.html.login(loginForm))
      }
    )
  }

  def home = Action { implicit request =>
    session.get("userId").map(UUID.fromString(_)).map{uid =>
      Ok(views.html.workspace(UserDO.listBooks(uid), bookForm))
    }.getOrElse(
	Unauthorized("Oops, you are not connected")
      )
  }

  def login = Action{ implicit request =>
    Ok(views.html.login(loginForm))
  }

  val loadFile = parse.raw

  def loadBook = Action(loadFile){implicit request =>
    (if (request.body.size > request.body.memoryThreshold){
      println("[INFO] created from File " + request.body.asFile.getPath)
      Some(BookDO.newBook(request.body.asFile))
    } else {
      println("[INFO] created from bytes")
      request.body.asBytes().map(BookDO.newBook(_))
    }).map{epub =>
      Cache.set("ebook", epub, 0)
      Ok(views.html.workspace(List[(String,Long)](), bookForm.fill(epub)))
    }.getOrElse{
      //With error message
      println("[ERROR] Could not load file")
      Ok(views.html.workspace(List[(String,Long)](),
        bookForm.withGlobalError("An error occurred while trying to load the file.")))
    }
  }

  def newBook = Action{ implicit request =>    
    bookForm.bindFromRequest.fold(
      errors => {
	      BadRequest(views.html.workspace(List[(String,Long)](), errors))
      },
      book => {
	      session.get("userId").map(UUID.fromString(_)).map{uid =>
          getCachedBook.map{cb =>
            val newbook = new Book(cb.bookId, book.bookTitle, book.bookAuthors, book.bookAuthors,
                      book.bookPublishers, book.bookPublishedDates, book.bookTags,
                      book.bookSummary, cb.bookParts)
println("My new book: " + newbook)
              BookDO.saveBook(newbook)
              UserDO.newUserBook(uid, newbook.bookId)
              Ok(views.html.workspace(UserDO.listBooks(uid), bookForm))
          }.getOrElse(
            Ok(views.html.workspace(UserDO.listBooks(uid),
              bookForm.withGlobalError("An error occurred while trying to save the file.")))
          )
        }.getOrElse(
          Unauthorized("Oops, you are not connected")
        )
      }
    )
  }

    def groups = Action{ implicit request =>
    session.get("userId").map(UUID.fromString(_)).map{uid =>
      Ok(views.html.groups(UserDO.userOwnedGroups(uid), UserDO.userIsMemberGroups(uid)))
    }.getOrElse(
	  Unauthorized("Oops, you are not connected")
	)    
  }

  def group(groupId:String) = Action{ implicit request =>
    cachedGroup(groupId).map{group =>
      val members = cachedGroupMembers(groupId)
      Ok(views.html.group(group.id.toString, group.name, members, memberForm))
    }.getOrElse(
      NotFound("Oops, the group you're looking for does not exists :(")
    )
  }

  def newGroupMember(groupId:String) = Action{ implicit request =>
    memberForm.bindFromRequest.fold(
      errors => cachedGroup(groupId).map{group =>
      	BadRequest(views.html.group(groupId, group.name, cachedGroupMembers(groupId), errors))
      }.get, 
      member => {
	      UserDO.newGroupMember(UUID.fromString(groupId), UUID.fromString(member._1), member._2)
	      Cache.set("groupMembers:"+groupId, null)
	      Redirect(routes.Application.group(groupId))
      }
    )
  }

  def newEzoomlayer = Action{implicit request =>
    val ezoombookid = UUID.randomUUID
    val layerid = UUID.randomUUID
    val userid = UUID.randomUUID
    Ok(views.html.ezoombookedit(None, ezoomlayerForm(ezoombookid, layerid, userid)))
  }

  def loadEzoomLayer = Action(parse.multipartFormData){request =>
    val ezoombookid = UUID.randomUUID
    val layerid = UUID.randomUUID
    val userid = UUID.randomUUID
    request.body.file("ezlfile").map{filePart =>
      val lines = scala.io.Source.fromFile(filePart.ref.file).getLines.toSeq
      books.util.Transformer(lines) match{
        case Right(layerData) =>
          val ezoombookTitle = (layerData \ "ezoombook_title").asOpt[String]
          val filledForm = ezoomlayerForm(ezoombookid, layerid, userid).bind(layerData)
//          println("[INFO] Ze doc: " + Json.stringify(layerData))
          filledForm.fold(
            errors =>{
//              println("[INFO] Error form: " + errors.errors.mkString("\n"))
              Ok(views.html.ezoombookedit(None, errors))
            },
            ezl => {
//              println("[INFO] ezl: " + ezl);
              Ok(views.html.ezoombookedit(None, ezoomlayerForm.fill(ezl)))
            }
          )
        case Left(error) =>
          println("[ERROR] " + error)
          Ok(views.html.ezoombookedit(None, ezoomlayerForm(ezoombookid, layerid, userid).
          withGlobalError("An error occurred while trying to load the file. " + error)))
      }
    }.getOrElse{
      println("[ERROR] oops!")
      Ok(views.html.ezoombookedit(None, ezoomlayerForm(ezoombookid, layerid, userid).
        withGlobalError("An error occurred while trying to load the file.")))
    }
  }

  private def cachedGroup(groupId:String):Option[Group] = {
    Cache.getOrElse("group:"+groupId, 0){
      UserDO.getGroupById(UUID.fromString(groupId))
    }
  }

  private def cachedGroupMembers(groupId:String):List[User] = {
    Cache.getAs[List[User]]("groupMembers:"+groupId) match{
      case Some(mlst) if mlst != null => mlst
      case _ => val ulst = UserDO.getGroupMembers(UUID.fromString(groupId))
	      Cache.set("groupMembers:"+groupId, ulst, 0)
	      ulst
    }
  }

  private def getCachedBook:Option[Book] = {
    Cache.getAs[Book]("ebook")
  }
}
