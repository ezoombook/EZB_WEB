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
import play.api.libs.json.Reads
import play.api.i18n.Messages
import org.apache.commons.io.IOUtils
import scala.xml.XML
import jp.t2v.lab.play2.auth.AuthElement

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
object EzoomBooks extends Controller with AuthElement with AuthConfigImpl with ContextProvider{

  val loadFile = parse.raw

  /**
   * Redirects to book edition view for creating a new Book
   * @return
   */
  def newBook = StackAction {implicit request =>
    Ok(views.html.bookedit(bookForm))
  }

  /**
   * Loads and scans a book in epub format to display its meta data in the book form
   */
  def loadBook = Action(loadFile){implicit request =>
    (if (request.body.size > request.body.memoryThreshold){
      println("[INFO] created from File " + request.body.asFile.getPath)
      val book = BookDO.newBook(request.body.asFile)
      println("Part title: " + book.bookParts)
      Some(book)
    } else {
      println("[INFO] created from bytes")
      request.body.asBytes().map(BookDO.newBook(_))
    }).map{epub =>
      Cache.set("ebook", epub, 0)
      Ok(views.html.bookedit(bookForm.fill(epub)))
    }.getOrElse{
      //With error message
      println("[ERROR] Could not load file")
      Ok(views.html.bookedit(
        bookForm.withGlobalError("An error occurred while trying to load the file.")))
    }
  }

  /**
   * Stores the book in the dabase
   */
  def saveBook = StackAction(AuthorityKey -> RegisteredUser){ implicit request =>
    bookForm.bindFromRequest.fold(
      errors => {
        println("[ERROR] Found errors in bookForm: " + errors)
        BadRequest(views.html.bookedit(errors))
      },
      book => {
        withUser{user =>
          getCachedBook.map{cb =>
            val newbook = new Book(cb.bookId, book.bookTitle, book.bookAuthors, book.bookAuthors,
              book.bookPublishers, book.bookPublishedDates, book.bookTags,
              book.bookSummary, cb.bookCover, cb.bookParts)
            println("My new book: " + newbook)
            BookDO.saveBook(newbook)
            //BookDO.saveBookParts(newbook)
            UserDO.newUserBook(user.id, newbook.bookId)
            Redirect(routes.EzoomBooks.readBook(newbook.bookId.toString))
          }.getOrElse(
            Ok(views.html.bookedit(
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
  def saveEditedBook = StackAction(AuthorityKey -> RegisteredUser){ implicit request =>
    bookForm.bindFromRequest.fold(
      errors => {
        BadRequest(views.html.bookedit(errors))
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

  def newEzoomBook(bookId:String) = StackAction{implicit request =>
    Ok(views.html.ezoombookedit(bookId, ezoomBookForm))
  }**/

  /**
   * Receives from the request an eZoomBook form and saves the new eZoomBook into the database.
   * Then, it displays the ezoomlayer edition form
   */
  def saveEzoomBook(bookId:String) = StackAction(AuthorityKey -> RegisteredUser){implicit request =>
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
   * Stores an ezoomlayer in the databasse
   */
  def saveEzoomlayer(ezbId:String) = StackAction(AuthorityKey -> RegisteredUser){implicit request =>
    withUser{user =>
      withEzoomBook(ezbId){ezb =>
        ezoomlayerForm(ezb.ezoombook_id,UUID.randomUUID,user.id).bindFromRequest.fold(
          errors => {
            BadRequest(views.html.ezoomlayeredit(ezb, None, errors,BookDO.getBook(ezb.book_id.toString), canEditEzb(ezbId) _ ))
          },
          ezl => {
            BookDO.saveLayer(ezl)
            println(s"[INFO] Layer ${ezl.ezoomlayer_id} successfully saved!")
            Redirect(routes.EzoomBooks.ezoomLayerEdit(ezbId, ezl.ezoomlayer_id.toString,false))
          }
        )
      }
    }
  }

  private def canEditEzb(ezbId:String)(user:User):Boolean = {
    BookDO.getEzoomBook(UUID.fromString(ezbId)).exists{ezb =>
      println("ezb id: " + ezb.ezoombook_id)
      println("user projects: " + Collaboration.getProjectsByUser(user.id).map(_.ezbId).mkString(","))

      ezb.ezoombook_owner == user.id.toString ||
      Collaboration.getProjectsByUser(user.id).exists(_.ezbId.exists(_ == ezb.ezoombook_id))
    }
  }

  /**
   * Displays the ezoomlayer edit form without specifying a ezoomlayer,
   * creating by default a new empty ezoomlayer.
   */
  def ezoomBookEdit(ezbId:String) = StackAction(AuthorityKey -> canEditEzb(ezbId) _ ){implicit request =>
    withUser{user =>
      withEzoomBook(ezbId){ezb =>
        BookDO.setWorkingEzb(ezb)
        val newEzlayer = EzoomLayer(UUID.randomUUID, ezb.ezoombook_id, 1,
          "user:"+user.id, books.dal.Status.workInProgress, false, List[String](), List[Contrib]())
        BookDO.setWorkingLayer(newEzlayer)
        val ezlform = ezoomlayerForm.fill(newEzlayer)
        Ok(views.html.ezoomlayeredit(ezb,
          Some(newEzlayer),
          ezoomlayerForm,
          BookDO.getBook(ezb.book_id.toString),
          canEditEzb(ezbId) _))
      }
    }
  }

  /**
   * Creates a new empty eZoomLayer with a given level,
   * then redirects the user to the ezb edition page.
   */
  def createEzoomLayer(ezbId:String, layerLevel:String, owner:String) = StackAction(AuthorityKey -> RegisteredUser){implicit request =>
    withUser{user =>
      val layerid = UUID.randomUUID()
      val newEzLayer = EzoomLayer(
        ezoomlayer_id = layerid,
        ezoombook_id = UUID.fromString(ezbId),
        ezoomlayer_level = layerLevel.toInt,
        ezoomlayer_owner = owner,
        ezoomlayer_status = books.dal.Status.workInProgress,
        ezoomlayer_locked = false,
        ezoomlayer_summaries = List[String](),
        ezoomlayer_contribs = List[Contrib]()
        )
      BookDO.saveLayer(newEzLayer)
      Redirect(routes.EzoomBooks.ezoomLayerEdit(ezbId, layerid.toString,false))
    }
  }

  /**
   * Displays the ezoomlayer edit form for an existing ezoomlayer
   */
  def ezoomLayerEdit(ezbId:String, ezlId:String, refresh:Boolean) = StackAction(AuthorityKey -> RegisteredUser){implicit request =>
    withUser{user =>
      withEzoomBook(ezbId){ezb =>
        BookDO.getEzoomLayer(UUID.fromString(ezlId), refresh).map{ezl =>
          val ezlform = ezoomlayerForm.fill(ezl)
          Ok(views.html.ezoomlayeredit(ezb, Some(ezl), ezlform,BookDO.getBook(ezb.book_id.toString), canEditEzb(ezbId))
          ).withSession(
            session + ("working-layer" -> ezl.ezoomlayer_id.toString)
          )
        }.getOrElse{
          NotFound("Oops! We couldn't find the EzoomLayer you are looking for :(")
        }
      }
    }
  }

  /**
   * If a working layer exists this action displays the edition page for that layer,
   * otherwise it shows the edition page without layer
   */
  def workingEzoomLayer = StackAction(AuthorityKey -> RegisteredUser){implicit request =>
    withUser{user =>
      BookDO.getWorkingLayer.flatMap{ezl =>
        BookDO.getWorkingEzb.map{ezb =>
          val ezlform = ezoomlayerForm.fill(ezl)
          Ok(views.html.ezoomlayeredit(ezb, Some(ezl), ezlform, BookDO.getBook(ezb.book_id.toString),
            canEditEzb(ezb.ezoombook_id.toString) _))
        }
      }.getOrElse{
        NotFound("Oops! We couldn't find the EzoomLayer you are looking for :(")
      }
    }
  }

  /**
   * Loads an ezoomlayer from a marked down file and displays it
   * in the ezoomlayer edition form
   */
  //TODO Correct repeated contribution_id on atomic contrib
  def loadEzoomLayer(ezbId:String) = Action(parse.multipartFormData){implicit request =>
    withUser{user =>
      withEzoomBook(ezbId){ezb =>
        val ezlform = ezoomlayerForm(ezb.ezoombook_id, UUID.randomUUID, user.id)
        request.body.file("ezlfile").map{filePart =>
          val lines = scala.io.Source.fromFile(filePart.ref.file).getLines.toSeq
          books.util.Transformer(lines) match{
            case Right(layerData) =>
              val ezoombookTitle = (layerData \ "ezoombook_title").asOpt[String]
              val filledForm = ezlform.bind(layerData)

              Ok(views.html.ezoomlayeredit(ezb, None, filledForm, BookDO.getBook(ezb.book_id.toString),
                canEditEzb(ezbId) _))
            case Left(error) =>
              println("[ERROR] " + error)
              Ok(views.html.ezoomlayeredit(ezb,
                None,
                ezlform.withGlobalError("An error occurred while trying to load the file. " + error),
                BookDO.getBook(ezb.book_id.toString),
                canEditEzb(ezbId) _))
          }
        }.getOrElse{
          println("[ERROR] oops!")
          Ok(views.html.ezoomlayeredit(ezb,
            None,
            ezlform.withGlobalError("An error occurred while trying to load the file."),
            BookDO.getBook(ezb.book_id.toString),
            canEditEzb(ezbId) _))
        }
      }
    }
  }

  //TODO rename to simply book
  def readBook(id:String) = StackAction(AuthorityKey -> Guest){implicit request =>
    BookDO.getBook(id).map{book =>
      val  ezb = Ezoombook (UUID.randomUUID, UUID.fromString(id) ,context.user.get.id.toString, books.dal.Status.workInProgress,"",false) 
      Ok(views.html.book(book, ezoomBookForm.fill(ezb), BookDO.getEzoomBooks(UUID.fromString(id))))
    }.getOrElse{
      println("[ERROR] Could not load book " + id)
      BadRequest(views.html.listbooks(BookDO.listBooks,bookForm))
    }
  }

  def bookCover(bookId:String) = StackAction(AuthorityKey -> Guest){implicit request =>
    val cover = BookDO.getBookCover(UUID.fromString(bookId))
//    val fis = new FileInputStream(new File("/Users/mayleen/ezoombook2.png"))
//    val cover = IOUtils.toByteArray(fis)
    if (cover.size > 0){
      Ok(cover).as(play.api.libs.MimeTypes.forExtension("png").getOrElse(play.api.http.MimeTypes.BINARY))
    }else{
      Redirect(routes.Assets.at("/images/bookcover.png"))
    }
  }

  def cachedBookCover = StackAction(AuthorityKey -> Guest){implicit request =>
    getCachedBook.map{cb =>
      if(!cb.bookCover.isEmpty)
        Ok(cb.bookCover).as(play.api.libs.MimeTypes.forExtension("png").getOrElse(play.api.http.MimeTypes.BINARY))
      else
        Redirect(routes.Assets.at("/images/bookcover.png"))
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

   def listbooks = StackAction(AuthorityKey -> Guest) {implicit request =>
    Ok(views.html.listbooks(BookDO.listBooks,bookForm))
  }

   def reedit(id:String) = StackAction(AuthorityKey -> RegisteredUser) {implicit request =>
     BookDO.getBook(id).map{b => 
      Cache.set("ebook",b,0)
      Ok(views.html.bookreedit(List[(String, Long)](),bookForm.fill(b)))
     }.getOrElse{
      println("[ERROR] " )
      BadRequest(views.html.bookreedit(List[(String, Long)](),bookForm.withGlobalError("An error occured")))
     }
  }

  def setReadingEzb(ezbId:String, part:String, layer:String) = StackAction(AuthorityKey -> Guest) {implicit request =>
    BookDO.getEzoomBook(UUID.fromString(ezbId)).flatMap{ezb =>
      BookDO.getBook(ezb.book_id.toString).map{book =>
        val readPart = if (!part.isEmpty) part else book.bookParts(0).partId
        Redirect(routes.EzoomBooks.readEzb(book.bookId.toString,readPart)).withSession(
          session + ("working-ezb" -> ezb.ezoombook_id.toString) + ("show-layer" -> layer)
        )
      }
    }.getOrElse{
      println("[WEB-ERROR] Could not load eZoomBook: " + ezbId)
      NotFound("Oops! We couldn't find the eZoomBook you are looking for")
    }
  }

  def readEzb(bookId:String,partId:String) = StackAction(AuthorityKey -> Guest) {implicit request =>
    BookDO.getBook(bookId).flatMap{book =>
      context.activeEzb.flatMap{ezbId =>
        BookDO.getEzoomBook(ezbId).map{ezb =>
          val partIndex = book.bookParts.indexWhere(_.partId == partId)
          val (styles,bodyContent) = BookDO.getPartContentAndStyle(book.bookId,partId)
          val layer = session.get("show-layer").getOrElse("0")
          Ok(views.html.read(book,
            ezb, partIndex,
            ezb.ezoombook_layers.foldLeft(Map[String,EzoomLayer]()){(lst, layer) =>
              BookDO.getEzoomLayer(UUID.fromString(layer._2)).
                map(ezl => lst + (layer._1 -> ezl)).getOrElse(lst)
            },
            play.api.templates.Html(bodyContent),
            play.api.templates.Html(styles),
            layer))
        }
      }
    }.getOrElse{
      NotFound("Oops! We couldn't find the resource you are looking for")
    }
  }

//  def bookPart(bookId:String,partId:String) = StackAction{implicit request =>
//    BookDO.getBookPart(UUID.fromString(bookId), partId).map{bpart =>
//      //TODO get extension corresponding to epub format : play.api.libs.MimeTypes.forExtension("txt")
//      Ok(bpart.content).as(play.api.http.MimeTypes.HTML)
//    }.getOrElse{
//      NotFound("Oops! We couldn't find the chapter you are looking for")
//    }
//  }

  def bookResource(bookId:String, file:String) = StackAction(AuthorityKey -> Guest){implicit request =>
    if (file.contains(".html")){
      Redirect(routes.EzoomBooks.readEzb(bookId, file))
    }else{
      val mtype = file.split('.').lastOption match{
        case Some(ext)  => play.api.libs.MimeTypes.forExtension(ext).getOrElse(play.api.http.MimeTypes.BINARY)
        case _ => play.api.http.MimeTypes.BINARY
      }
      val res = BookDO.getBookResource(UUID.fromString(bookId),file)

      if (res.size > 0){
        Ok(res).as(mtype)
      }else{
        Redirect(routes.Assets.at("/images/bookcover.png"))
      }
    }
  }

  def readLayer(bookId:String,partId:String) = StackAction(AuthorityKey -> Guest){implicit request =>
    BookDO.getBook(bookId).map{book =>
      val partIndex = book.bookParts.indexWhere(_.partId == partId)
      val ezoomlayeropt = context.activeLayer.flatMap(BookDO.getEzoomLayer(_))

      val quoteRanges:List[String] = ezoomlayeropt.flatMap{ezl =>
        ezl.ezoomlayer_contribs.find(_.part_id == Some(partId)).map{
          case part:EzlPart =>
            for(pc <- part.part_contribs if pc.contrib_type == "contrib.Quote") yield {pc.range.getOrElse("")}
        }
      }.getOrElse{ List[String]() }

      val (styles,bodyContent) = BookDO.getPartContentAndStyle(book.bookId,partId)

      Ok(views.html.bookread(book,ezoomlayeropt,
        partIndex,partId, quoteRanges,
        play.api.templates.Html(bodyContent),
        play.api.templates.Html(styles)))
    }.getOrElse{
      NotFound("Oops! We couldn't find the chapter you are looking for")
    }
  }

  //TODO define in special BodyParser
  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  implicit val quoteReads: Reads[AtomicContrib] = (
    (__ \ 'userid).read[String] ~
    (__ \ 'bookid).read[String] ~
    (__ \ 'ezbid).read[String] ~
    (__ \ 'layerid).read[String] ~
    (__ \ 'partid).readNullable[String] ~
    (__ \ 'content).read[String] ~
    (__ \ 'range).readNullable[String]
    )((uid,bookId,ezbId,layerId,partId,content,range) =>
      AtomicContrib("quote:"+UUID.randomUUID,"contrib.Quote",UUID.fromString(layerId),UUID.fromString(ezbId),
        UUID.fromString(uid),partId,range,books.dal.Status.workInProgress,false,content))

  /**
   * Receives a Json document containing a quote and adds it to the database
   * TODO Add authorizied access restriction
   * @return
   */
  def addQuote = Action(parse.json){ request =>
    request.body.validate[AtomicContrib].map{
      case contrib =>
        val ezl = BookDO.getEzoomLayer(contrib.ezoomlayer_id).getOrElse(
          EzoomLayer(contrib.ezoomlayer_id, contrib.ezoombook_id, 1,
            contrib.user_id.toString, books.dal.Status.workInProgress, false, List[String](), List())
        )
        val partTitle:String = if(!contrib.part_id.isEmpty){
          BookDO.getEzoomBook(ezl.ezoombook_id).flatMap{ezb =>
            BookDO.getBook(ezb.book_id.toString).flatMap{book =>
              book.bookParts.find(_.partId == contrib.part_id.get).map{part =>
                part.title.getOrElse("")
              }
            }
          }.getOrElse("")
        }else{""}

        val modifedLayer = EzoomLayer.updatePart(ezl,contrib.part_id.getOrElse(""),partTitle,contrib)
        BookDO.saveLayer(modifedLayer)
        Ok("Quote saved!")
    }.recoverTotal{
      e => {
        println("[ERROR] Detected error:"+ JsError.toFlatJson(e))
        BadRequest("Detected error:"+ JsError.toFlatJson(e))
      }
    }
  }

  def ezoomLayerDelete(ezbId:String, layerLevel:Int) = StackAction(AuthorityKey -> RegisteredUser){implicit request =>
    BookDO.deleteEzoomLayer(UUID.fromString(ezbId), layerLevel)
    Redirect(routes.EzoomBooks.ezoomBookEdit(ezbId))
  }

  def ezoomBookDelete(ezbId:String) = StackAction(AuthorityKey -> RegisteredUser){implicit request =>
    withUser{user =>
      //TODO Validate that user has the right to delete ezb
      BookDO.deleteEzoomBook(UUID.fromString(ezbId))
      Redirect(routes.Workspace.home)
    }
  }

  def getContribution(layerId:String, contribId:String) = StackAction(AuthorityKey -> Guest){request =>
    BookDO.getEzoomLayer(UUID.fromString(layerId)).flatMap{layer =>
      layer.ezoomlayer_contribs.collectFirst{
        case atomic:AtomicContrib if(atomic.contrib_id == contribId) => atomic
        case part:EzlPart if(part.part_contribs.exists(_.contrib_id == contribId)) =>
            part.part_contribs.find(_.contrib_id == contribId).get
      }.map{contrib =>
        Ok(Json.toJson(contrib.copy(contrib_type = Messages("application."+contrib.contrib_type))))
      }
    }.getOrElse{
      NotFound(s"Contribution $layerId:$contribId not found")
    }
  }

  def withEzoomBook(ezbId:String)(block:(Ezoombook) => Result):Result = {
    BookDO.getEzoomBook(UUID.fromString(ezbId)).map{ezb =>
      block(ezb)
    }.getOrElse{
      NotFound("Oops! We couldn't find the EzoomBook you are looking for :(")
    }
  }

  /**
  def addezbtrl(ezbId:String) = StackAction {implicit request =>
      BookDO.getEzoomBook(ezbId).map {
        abook =>
         readinglist = readinglist +: abook
      }.getOrElse(
        Unauthorized("Oops! that is not a valid page!")
      )
  }
    */

  /**
  def addezbtf(ezbId:String) = StackAction {implicit request =>
      BookDO.getEzoomBook(ezbId).map {
        abook =>
         favorite = favorite +: abook
      }.getOrElse(
        Unauthorized("Oops! that is not a valid page!")
      )
  }
    */

}
