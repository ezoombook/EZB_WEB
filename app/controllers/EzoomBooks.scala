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

import java.io._
import java.util.UUID
import play.api._
import play.api.mvc._
import play.api.data._
import Forms._
import play.api.libs.json.Json
import org.apache.commons.io.IOUtils

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
        println("[ERROR] Found errors in bookForm: " + errors)
        BadRequest(views.html.bookedit(List[(String,Long)](), errors))
      },
      book => {
        withUser{user =>
          getCachedBook.map{cb =>
            val newbook = new Book(cb.bookId, book.bookTitle, book.bookAuthors, book.bookAuthors,
              book.bookPublishers, book.bookPublishedDates, book.bookTags,
              book.bookSummary, cb.bookCover, cb.bookParts)
            println("My new book: " + newbook)
            BookDO.saveBook(newbook)
            BookDO.saveBookParts(newbook)
            UserDO.newUserBook(user.id, newbook.bookId)
            Redirect(routes.EzoomBooks.readBook(newbook.bookId.toString))
          }.getOrElse(
            Ok(views.html.bookedit(UserDO.listBooks(user.id),
              bookForm.withGlobalError("An error occurred while trying to save the file.")))
          )
        }
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

  def newEzoomBook(bookId:String) = Action{implicit request =>
    Ok(views.html.ezoombookedit(bookId, ezoomBookForm))
  }**/

  /**
   * Receives from the request an eZoomBook form and saves the new eZoomBook into the database.
   * Then, it displays the ezoomlayer edition form
   */
  def saveEzoomBook(bookId:String) = Action{implicit request =>
    withUser{user =>
      ezoomBookForm.bindFromRequest.fold(
        errors => {
          println("[ERROR] Errors in ezoombookForm: " + errors)
          BookDO.getBook(bookId).map{book =>
            BadRequest(views.html.book(book,errors, BookDO.getEzoomBooks(UUID.fromString(bookId))))
          }.get
        },
        ezb => {
          BookDO.saveEzoomBook(ezb)
          Redirect(routes.EzoomBooks.ezoomBookEdit(ezb.ezoombook_id.toString))
        }
      )
    }
  }

  /**
   * Displays the ezoomlayer edition form
   */
//  def newEzoomlayer(ezbId:String) = Action{implicit request =>
//    Redirect(routs.EzoomBooks.ezoomBookEdit(ezbId))
//  }

  /**
   * Stores an ezoomlayer in the databasse
   */
  def saveEzoomlayer(ezbId:String) = Action{implicit request =>
    withUser{user =>
      withEzoomBook(ezbId){ezb =>
        ezoomlayerForm(ezb.ezoombook_id,UUID.randomUUID,user.id).bindFromRequest.fold(
          errors => {
            BadRequest(views.html.ezoomlayeredit(ezb, errors))
          },
          ezl => {
            BookDO.saveLayer(ezl)
            Redirect(routes.EzoomBooks.ezoomLayerEdit(ezbId, ezl.ezoomlayer_id.toString))
          }
        )
      }
    }
  }

  /**
   * Displays the ezoomlayer edit form without specifying a ezoomlayer
   */
  def ezoomBookEdit(ezbId:String) = Action{implicit request =>
    withUser{user =>
      withEzoomBook(ezbId){ezb =>
        val ezlform = ezoomlayerForm.bind(
          Map("ezoombook_id" -> ezb.ezoombook_id.toString,
            "ezoomlayer_id" -> UUID.randomUUID.toString,
            "ezoomlayer_owner" -> user.id.toString)
        )

        Ok(views.html.ezoomlayeredit(ezb, ezoomlayerForm))
      }
    }
  }

  /**
   * Displays the ezoomlayer edit form for an existing ezoomlayer
   */
  def ezoomLayerEdit(ezbId:String, ezlId:String) = Action{implicit request =>
    withUser{user =>
      withEzoomBook(ezbId){ezb =>
        BookDO.getEzoomLayer(UUID.fromString(ezlId)).map{ezl =>
          val ezlform = ezoomlayerForm.fill(ezl)
          Ok(views.html.ezoomlayeredit(ezb, ezlform))
        }.getOrElse{
          NotFound("Oops! We couldn't find the EzoomLayer you are looking for :(")
        }
      }
    }
  }
  

  /**
   * Loads an ezoomlayer from a marked down file and displays it
   * in the ezoomlayer edition form
   */
  def loadEzoomLayer(ezbId:String) = Action(parse.multipartFormData){implicit request =>
    withUser{user =>
      withEzoomBook(ezbId){ezb =>
        val ezlform = ezoomlayerForm.bind(
          Map("ezoombook_id" -> ezb.ezoombook_id.toString,
            "ezoomlayer_id" -> UUID.randomUUID.toString,
            "ezoomlayer_owner" -> user.id.toString)
        )

        request.body.file("ezlfile").map{filePart =>
          val lines = scala.io.Source.fromFile(filePart.ref.file).getLines.toSeq
          books.util.Transformer(lines) match{
            case Right(layerData) =>
              val ezoombookTitle = (layerData \ "ezoombook_title").asOpt[String]
              val filledForm = ezlform.bind(layerData)
              //          println("[INFO] Ze doc: " + Json.stringify(layerData))
              Ok(views.html.ezoomlayeredit(ezb, filledForm))
//              filledForm.fold(
//                errors =>{
//                  //              println("[INFO] Error form: " + errors.errors.mkString("\n"))
//                  Ok(views.html.ezoomlayeredit(Some(ezb), errors))
//                },
//                ezl => {
//                  //              println("[INFO] ezl: " + ezl);
//                  Ok(views.html.ezoomlayeredit(Some(ezb), ezoomlayerForm.fill(ezl)))
//                }
//              )
            case Left(error) =>
              println("[ERROR] " + error)
              Ok(views.html.ezoomlayeredit(ezb,
                ezlform.withGlobalError("An error occurred while trying to load the file. " + error)))
          }
        }.getOrElse{
          println("[ERROR] oops!")
          Ok(views.html.ezoomlayeredit(ezb,
            ezlform.withGlobalError("An error occurred while trying to load the file.")))
        }
      }
    }
  }

  def readBook(id:String) = Action{implicit request =>
    BookDO.getBook(id).map{book =>
      val  ezb = Ezoombook (UUID.randomUUID, UUID.fromString(id) ,context.user.get.id.toString, books.dal.Status.workInProgress,"",false) 
      Ok(views.html.book(book, ezoomBookForm.fill(ezb), BookDO.getEzoomBooks(UUID.fromString(id))))
    }.getOrElse{
      println("[ERROR] Could not load book " + id)
      BadRequest(views.html.listbooks(BookDO.listBooks,bookForm))
    }
  }

  def bookCover(bookId:String) = Action{implicit request =>    
    val cover = BookDO.getBookCover(UUID.fromString(bookId))
//    val fis = new FileInputStream(new File("/Users/mayleen/ezoombook2.png"))
//    val cover = IOUtils.toByteArray(fis)
    if (cover.size > 0){
      Ok(cover).as(play.api.libs.MimeTypes.forExtension("png").getOrElse(play.api.http.MimeTypes.BINARY))
    }else{
      Redirect(routes.Assets.at("/images/bookcover.png"))
    }
  }

  def cachedBookCover = Action{implicit request =>
    getCachedBook.map{cb =>
      Ok(cb.bookCover).as(play.api.libs.MimeTypes.forExtension("png").getOrElse(play.api.http.MimeTypes.BINARY))
    }.getOrElse{
      Redirect(routes.Assets.at("/images/bookcover.png"))
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

  def readEzb(ezbId:String) = Action {implicit request =>
    val ezbuuid = UUID.fromString(ezbId)
    Ok(views.html.read())
  }

  def withEzoomBook(ezbId:String)(block:(Ezoombook) => Result):Result = {
    BookDO.getEzoomBook(UUID.fromString(ezbId)).map{ezb =>
      block(ezb)
    }.getOrElse{
      NotFound("Oops! We couldn't find the EzoomBook you are looking for :(")
    }
  }
}
