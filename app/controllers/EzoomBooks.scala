package controllers

import forms.EzbForms
import EzbForms._
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
object EzoomBooks extends Controller with ContextProvider{

  val loadFile = parse.raw

  /**
   * Redirects to book edition view for creating a new Book
   * @return
   */
  def newBook = Action {implicit request =>
    Ok(views.html.bookedit(List[(String, Long)](),bookForm))
  }

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
      Ok(views.html.bookedit(List[(String,Long)](), bookForm.fill(epub)))
    }.getOrElse{
      //With error message
      println("[ERROR] Could not load file")
      Ok(views.html.bookedit(List[(String,Long)](),
        bookForm.withGlobalError("An error occurred while trying to load the file.")))
    }
  }

  /**
   * Stores the book in the dabase
   */
  def saveBook = Action{ implicit request =>
    bookForm.bindFromRequest.fold(
      errors => {
        BadRequest(views.html.bookedit(List[(String,Long)](), errors))
      },
      book => {
        session.get("userId").map(UUID.fromString(_)).map{uid =>
          getCachedBook.map{cb =>
            val newbook = new Book(cb.bookId, book.bookTitle, book.bookAuthors, book.bookAuthors,
              book.bookPublishers, book.bookPublishedDates, book.bookTags,
              book.bookSummary, cb.bookParts)
            println("My new book: " + newbook)
            BookDO.saveBook(newbook)
            BookDO.saveBookParts(newbook)
            UserDO.newUserBook(uid, newbook.bookId)

            Ok(views.html.bookedit(UserDO.listBooks(uid), bookForm))
          }.getOrElse(
            Ok(views.html.bookedit(UserDO.listBooks(uid),
              bookForm.withGlobalError("An error occurred while trying to save the file.")))
          )
        }.getOrElse(
          Unauthorized("Oops, you are not connected")
        )
      }
    )
  }

  /**
   * Save the modifications made to a book meta-data.
   * Parts are not saved
   */
  def saveEditedBook = Action{ implicit request =>
    bookForm.bindFromRequest.fold(
      errors => {
        BadRequest(views.html.bookedit(List[(String,Long)](), errors))
      },
      book => {
        BookDO.saveBook(book)
        Ok(views.html.listbooks(BookDO.listBooks,bookForm))
      }
    )
  }

  /**
   * Displays the ezoombook edition form for creating a new eZoomBook
   * for an existing book.
   * @param The id of the book
   */
  def newEzoomBook(bookId:String) = Action{implicit request =>
    Ok(views.html.ezoombookedit(bookId, ezoomBookForm))
  }

  /**
   * Receives from the request an eZoomBook form and saves the new eZoomBook into the database
   */
  def saveEzoomBook = Action{implicit request =>
    ezoomBookForm.bindFromRequest.fold(
      errors => {
        BadRequest(views.html.ezoombookedit(errors))
      },
      ezb => {
        BookDO.saveEzoomBook(ezb)
        Ok(views.html.ezoombookedit(ezoomBookForm.fill(ezb)))
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
    Ok(views.html.ezoomlayeredit(None, ezoomlayerForm))
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
        BadRequest(views.html.ezoomlayeredit(None, errors))
      },
      ezl => {
        try{
          //println("EZB ok!!" + Json.toJson(ezl))
          BookDO.saveLayer(ezl)
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
  def loadEzoomLayer = Action(parse.multipartFormData){implicit request =>
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

  def readBook(id:String) = Action{implicit request =>
    BookDO.getBook(id).map(book =>
      Ok(views.html.book(book))
    ).getOrElse{
      println("[ERROR] Could not load book " + id)
      BadRequest(views.html.listbooks(BookDO.listBooks,bookForm))
    }
  }

  /**
   * Gets the current working book from the cache
   * @return
   */
  private def getCachedBook:Option[Book] = {
    Cache.getAs[Book]("ebook")
  }

   def listbooks = Action {implicit request =>
    Ok(views.html.listbooks(BookDO.listBooks,bookForm))
  }

   def reedit(id:String) = Action {implicit request =>
     BookDO.getBook(id).map{b => 
      Cache.set("ebook",b,0)
      Ok(views.html.bookreedit(List[(String, Long)](),bookForm.fill(b)))
     }.getOrElse{
      println("[ERROR] " )
      BadRequest(views.html.bookreedit(List[(String, Long)](),bookForm.withGlobalError("An error occured")))
     }
  }

}
