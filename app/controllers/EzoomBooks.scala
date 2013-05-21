package controllers

import controllers.EzbForms._
import models._
import users.dal._
import books.dal._

import play.api._
import scala.Some
import Play.current
import cache.Cache

import java.util.UUID
import play.api._
import play.api.mvc._
import play.api.data._
import Forms._
import play.api.libs.json.Json

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 15/05/13
 * Time: 16:40
 * To change this template use File | Settings | File Templates.
 */

/**
 * Manage ezoombook related operations: book upload, ezoombook creation and edition, etc
 */
object EzoomBooks extends Controller{

  val loadFile = parse.raw

  /**
   * Loads and scans a book in epub format to display its meta data in the book form
   */
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

  /**
   * Stores the book in the dabase
   */
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

  /**
   * Displays the ezoomlayer edition form
   */
  def newEzoomlayer = Action{implicit request =>
    val ezoombookid = UUID.randomUUID
    val layerid = UUID.randomUUID
    val userid = UUID.randomUUID
    Ok(views.html.ezoombookedit(None, ezoomlayerForm))
  }

  /**
   * Stores an ezoomlayer in the databasse
   */
  def saveEzoomlayer = Action{implicit request =>
    val ezoombookid = UUID.randomUUID //TODO from session
    val layerid = UUID.randomUUID //TODO from session
    val userid = UUID.randomUUID //TODO from session
    ezoomlayerForm(ezoombookid, layerid, userid).bindFromRequest.fold(
      errors => {
        println("oops: " + errors.errors)
        BadRequest(views.html.ezoombookedit(None, errors))
      },
      ezl => {
        try{
          println("EZB ok!!" + Json.toJson(ezl))
        }catch{
          case e => println("[ERROR] Oops caught an exception while parsing object:")
                    e.printStackTrace()
        }

//        BookDO.saveLayer(ezl)
        Ok(views.html.ezoombookedit(None, ezoomlayerForm.fill(ezl)))
      }
    )
  }

  /**
   * Loads an ezoomlayer from a marked down file and displays it
   * in the ezoomlayer edition form
   */
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

  /**
   * Gets the current working book from the cache
   * @return
   */
  private def getCachedBook:Option[Book] = {
    Cache.getAs[Book]("ebook")
  }

}