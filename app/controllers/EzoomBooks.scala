package controllers

import _root_.books.dal.EzlPart
import _root_.java.util.UUID
import forms.EzbForms
import EzbForms._
import models._
import users.dal._
import books.dal._
import project.dal._

import scala.util._
import play.api._
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
object EzoomBooks extends Controller with AuthElement with AuthConfigImpl with ContextProvider {

  val loadFile = parse.raw

  /**
   * Redirects to book edition view for creating a new Book
   * @return
   */
  def newBook = StackAction(AuthorityKey -> RegisteredUser) {
    implicit request =>
      Ok(views.html.bookedit(bookForm))
  }

  /**
   * Loads and scans a book in epub format to display its meta data in the book form
   */
  def loadBook = Action(loadFile) {
    implicit request =>
      (if (request.body.size > request.body.memoryThreshold) {
        Logger.debug("Book created from File")
        val book = BookDO.newBook(request.body.asFile)
        Some(book)
      } else {
        Logger.debug("Book created from bytes")
        request.body.asBytes().map(BookDO.newBook(_))
      }).map {
        epub =>
          Cache.set("ebook", epub, 0)
          Ok(views.html.bookedit(bookForm.fill(epub)))
      }.getOrElse {
        //With error message
        Logger.error("Could not load book file")
        Ok(views.html.bookedit(
          bookForm.withGlobalError("An error occurred while trying to load the file.")))
      }
  }

  /**
   * Stores the book in the dabase
   */
  def saveBook = StackAction(AuthorityKey -> RegisteredUser) {
    implicit request =>
      bookForm.bindFromRequest.fold(
        errors => {
          println("[ERROR] Found errors in bookForm: " + errors)
          BadRequest(views.html.bookedit(errors))
        },
        book => {
          withUser {
            user =>
              getCachedBook.map {
                cb =>
                  val newbook = new Book(cb.bookId, book.bookTitle, book.bookAuthors, book.bookAuthors,
                    book.bookPublishers, book.bookPublishedDates, book.bookTags,
                    book.bookSummary, cb.bookCover, cb.bookParts)
                  BookDO.saveBook(newbook)
                  //BookDO.saveBookParts(newbook)
                  UserDO.newUserBook(user.id, newbook.bookId)
                  Redirect(routes.EzoomBooks.listbooks)
              }.getOrElse(
                BadRequest(views.html.bookedit(
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
  def saveEditedBook = StackAction(AuthorityKey -> RegisteredUser) {
    implicit request =>
      bookForm.bindFromRequest.fold(
        errors => {
          BadRequest(views.html.bookedit(errors))
        },
        book => {
          BookDO.saveBook(book)
          Ok(views.html.listbooks(BookDO.listBooks, bookForm))
        }
      )
  }

  /**
   * Receives from the request an eZoomBook form and saves the new eZoomBook into the database.
   * Then, it displays the ezoomlayer edition form
   */
  def saveEzoomBook(bookId: String) = StackAction(AuthorityKey -> RegisteredUser) {
    implicit request =>
      withUser {
        user =>
          ezoomBookForm.bindFromRequest.fold(
            errors => {
              println("[ERROR] Errors in ezoombookForm: " + errors)
              BookDO.getBook(bookId).map {
                book =>
                  BadRequest(views.html.book(book, errors, BookDO.getEzoomBooks(UUID.fromString(bookId))))
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
  //def saveEzoomlayer(ezbId: String) = StackAction(AuthorityKey -> canEditEzb(ezbId) _) {
  def saveEzoomlayer(ezbId: String) = StackAction(AuthorityKey -> RegisteredUser) {
    implicit request =>
      saveEzoomLayerAction(None, ezbId)
  }

  //def saveProjEzoomLayer(projId:String, ezbId: String) = StackAction(AuthorityKey -> canEditProjectEzb(projId) _){
  def saveProjEzoomLayer(projId:String, ezbId: String) = StackAction(AuthorityKey -> RegisteredUser){
    implicit request =>
      saveEzoomLayerAction(Some(projId), ezbId)
  }

  private def saveEzoomLayerAction[A](projId:Option[String], ezbId: String)(implicit request:Request[A]) = {
    withUser {
      user =>
        withEzoomBook(ezbId) {
          ezb =>
            val projOpt = projId.flatMap{pid => BookDO.getProject(UUID.fromString(pid))}
            val activeLayer = context.activeLayer.flatMap(BookDO.getEzoomLayer(_))

            ezoomlayerForm(ezb.ezoombook_id, UUID.randomUUID, user.id).bindFromRequest.fold(
              errors => {
                BadRequest(views.html.ezoomlayeredit(ezb, activeLayer,
                  errors, BookDO.getBook(ezb.book_id.toString), projOpt,
                  canEditProjectLayer(ezb, projOpt, activeLayer.map(_.ezoomlayer_id.toString).getOrElse("")) _,
                  canEditProjContrib(ezb, activeLayer, projOpt) _))
              },
              ezl => {
                val newezb = if (ezl.ezoomlayer_status == books.dal.Status.published) {
                  ezb.copy(ezoombook_status = books.dal.Status.published, ezoombook_public = true)
                } else {
                  ezb
                }
                BookDO.saveEzoomBook(newezb)

                //Save whole layer or only assigned part(s) according to the role of the user
                val modifedLayer =
                  if(canEditProjectLayer(ezb, projOpt, ezl.ezoomlayer_id.toString)(user)){
                    ezl
                  }else{
                    (for{
                      proj <- projOpt
                      member <- proj.projectTeam.find(_.userId == user.id)
                      layer <- BookDO.getEzoomLayer(ezl.ezoomlayer_id, true)
                      contrib <- ezl.ezoomlayer_contribs.find(_.part_id.exists(_ == member.assignedPart))
                    } yield (contrib match{
                        case modifiedPart:EzlPart => updateEzlPart(layer, modifiedPart)
                        case _ =>
                          Logger.debug("Could not find part with id " + member.assignedPart)
                          layer
                      })).getOrElse(ezl)
                  }

                BookDO.saveLayer(modifedLayer)
                Logger.debug(s"Layer ${ezl.ezoomlayer_id} successfully saved!")
                Ok( views.html.ezoomlayeredit(ezb, Some(modifedLayer),
                  ezoomlayerForm.fill(modifedLayer), BookDO.getBook(ezb.book_id.toString), projOpt,
                  canEditProjectLayer(ezb, projOpt, modifedLayer.ezoomlayer_id.toString) _,
                  canEditProjContrib(ezb, Some(modifedLayer), projOpt) _)
                ).withSession(
                    session + (WORKING_LAYER -> modifedLayer.ezoomlayer_id.toString)
                  )
              }
            )
        }
    }
  }

  private def updateEzlPart(ezl:EzoomLayer, newPart:EzlPart):EzoomLayer = {
    def updateContribList(cList:List[Contrib], acumList:List[Contrib]):List[Contrib] = {
      cList match{
        case Nil =>
          acumList :+ newPart
        case contrib::rest =>
          contrib match{
            case part:EzlPart if part.contrib_id == newPart.contrib_id =>
              (acumList :+ newPart) ++ rest
            case _ => updateContribList(rest, acumList :+ contrib)
          }
      }
    }

    EzoomLayer.contribsL.set(ezl, updateContribList(ezl.ezoomlayer_contribs, List[Contrib]()))
  }

  private def canEditEzb(ezbId: String)(user: User): Boolean = {
    BookDO.getEzoomBook(UUID.fromString(ezbId)).exists {
      ezb =>
        ezb.ezoombook_owner == user.id.toString ||
          Collaboration.getProjectsByUser(user.id).exists(_.ezbId.exists(_ == ezb.ezoombook_id))
    }
  }

  /**
   * Displays the ezoomlayer edit form without specifying a ezoomlayer,
   * creating by default a new empty ezoomlayer.
   */
  //TODO Verify this works well without project
  //def ezoomBookEdit(ezbId: String) = StackAction(AuthorityKey -> canEditEzb(ezbId) _) {
  def ezoomBookEdit(ezbId: String) = StackAction(AuthorityKey -> RegisteredUser) {
    implicit request =>
      withUser {
        user =>
          withEzoomBook(ezbId) {
            ezb =>
              val newEzlayer = EzoomLayer(UUID.randomUUID, ezb.ezoombook_id, 1,
                "user:" + user.id, books.dal.Status.workInProgress, false, List[String](), List[Contrib]())
              val ezlform = ezoomlayerForm.fill(newEzlayer)
              Ok(views.html.ezoomlayeredit(ezb,
                Some(newEzlayer),
                ezoomlayerForm,
                BookDO.getBook(ezb.book_id.toString),
                None,
                canEditEzb(ezbId) _,
                canEditProjContrib(ezb, None, None) _ ))
          }
      }
  }

  /**
   * Creates a new empty eZoomLayer with a given level,
   * then redirects the user to the ezb edition page.
   */
//  def createEzoomLayer(ezbId: String, layerLevel: String, assignedPart: String, groupId: String) =
//    StackAction(AuthorityKey -> canEditEzb(ezbId) _) {

  def createEzoomLayer(ezbId: String, layerLevel: String, assignedPart: String, groupId: String) =
    StackAction(AuthorityKey -> RegisteredUser) {
      implicit request =>
        withUser {
          user =>
            val layerid = UUID.randomUUID()
            val owner = if (assignedPart.isEmpty) user.id.toString else "group:" + groupId
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
            Redirect(routes.EzoomBooks.ezoomLayerEdit(ezbId, layerid.toString))
        }
    }

  /**
   * Displays the ezoomlayer edit form for an existing ezoomlayer
   */
  //def ezoomLayerEdit(ezbId: String, ezlId: String) = StackAction(AuthorityKey -> canEditEzb(ezbId) _) {
  def ezoomLayerEdit(ezbId: String, ezlId: String) = StackAction(AuthorityKey -> RegisteredUser) {
    implicit request =>
          withEzoomBook(ezbId) {
            ezb =>
                BookDO.getEzoomLayer(UUID.fromString(ezlId), true).map {
                ezl =>
                  val ezlform = ezoomlayerForm.fill(ezl)
                  Ok(views.html.ezoomlayeredit(ezb, Some(ezl),
                    ezlform, BookDO.getBook(ezb.book_id.toString), None,
                    canEditProjectLayer(ezb, None, ezl.ezoomlayer_id.toString) _,
                    canEditProjContrib(ezb, Some(ezl), None) _ )
                  ).withSession(
                      session + (WORKING_LAYER -> ezl.ezoomlayer_id.toString)
                  )

              }.getOrElse {
                NotFound("Oops! We couldn't find the EzoomLayer you are looking for :(")
              }
          }
      }

  private def findContrib(ezl:EzoomLayer, partId:String, contribId:String):Option[Contrib] = {
    if(!partId.isEmpty){
      ezl.ezoomlayer_contribs.find(_.part_id.exists(_ == partId)).flatMap{
        case part:EzlPart =>
          if (contribId != part.contrib_id)
            part.part_contribs.find(_.contrib_id == contribId)
          else
            Some(part)
        case _ => None
      }
    }else{
      ezl.ezoomlayer_contribs.find(_.contrib_id == contribId)
    }
  }

  /**
   * Validates that a user can make modifications on a layer, like publishing it, adding parts or summaries, etc
   */
  private def canEditProjectLayer(ezb: Ezoombook, projOpt: Option[EzbProject], layerId: String)(user: User): Boolean = {
    ezb.ezoombook_owner == user.id.toString ||
    projOpt.exists(proj =>
      proj.projectOwnerId.toString == user.id.toString ||
        proj.isMultiLevel &&
          proj.projectTeam.exists(m => m.userId == user.id &&
            ezb.ezoombook_layers.get(m.assignedLayer).exists(_ == layerId)))
  }

  private def canEditProjectEzb(projId: String)(user: User): Boolean = {
    projId.toUUID.fold(
      err => {
        Logger.error(err); false
      },
      pid =>
        BookDO.getProject(pid).map{
          proj =>
            (proj.projectOwnerId == user.id) ||
             proj.projectTeam.find(_.userId == user.id).nonEmpty
        }.getOrElse(false)
    )
  }

  private def canEditProjContrib(ezb: Ezoombook, ezlOpt:Option[EzoomLayer], projOpt:Option[EzbProject])(user:User,
    partId:String, contribId:String):Boolean = {

    ezb.ezoombook_owner == user.id.toString ||
    ezlOpt.flatMap(ezl => findContrib(ezl, partId, contribId)).exists{contrib =>
      contrib.user_id == user.id ||
      projOpt.exists( proj =>
        proj.projectOwnerId.toString == user.id.toString ||
        UserDO.getGroupMembers(proj.groupId).exists{
          case (usr, role) => usr.id == user.id && role == AppDB.dal.Roles.coordinator
        } ||
        proj.projectTeam.find(_.userId == user.id).exists(_.assignedPart == partId))
    }
  }

  //def projectEzlayerEdit(projId: String) = StackAction(AuthorityKey -> canEditProjectEzb(projId) _) {
  def projectEzlayerEdit(projId: String) = StackAction(AuthorityKey -> RegisteredUser) {
    implicit request =>
      projId.toUUID.fold(
        err => BadRequest("Invalid projectId..."),
        pid => {
          BookDO.getProject(pid).map {
            proj =>
              withUserAndEzb(request, proj.ezoombookId.map(_.toString).getOrElse("")) {
                (user, ezb) =>
                  val member = proj.projectTeam.find(_.userId == user.id)
                  val bookOpt = BookDO.getBook(ezb.book_id.toString)

                  val layer = (((level:String) => for {
                    ezblayer <- ezb.ezoombook_layers.get(level)
                    layerId <- ezblayer.toUUID.right.toOption
                    layr <- BookDO.getEzoomLayer(layerId, true)
                  } yield layr)(
                      if(proj.projectOwnerId == user.id)
                        "1"
                      else {
                        member.map(_.assignedLayer).getOrElse("1")
                      }
                  )).getOrElse(new EzoomLayer(ezb.ezoombook_id,"group:"+proj.groupId))

                  val newLayer = (for{m <- member
                      book <- bookOpt
                      bookPart <- getBookPart(book, m.assignedPart)
                      nl <- createPart(proj, m, layer, bookPart)
                  } yield (nl)).getOrElse(layer)

                  Ok(views.html.ezoomlayeredit(ezb,
                    Some(newLayer),
                    ezoomlayerForm.fill(newLayer),
                    bookOpt,
                    Some(proj),
                    canEditProjectLayer(ezb, Some(proj), newLayer.ezoomlayer_id.toString),
                    canEditProjContrib(ezb, Some(newLayer), Some(proj))
                  )).withSession(
                      session + (WORKING_LAYER -> newLayer.ezoomlayer_id.toString)
                    )
              }
          }.getOrElse(
            NotFound("Oops! We couldn't find the project you are looking for :(")
          )
        }
      )
  }

  private def createPart(proj:EzbProject, member:TeamMember, layer:EzoomLayer, bookPart:BookPart):Option[EzoomLayer] = {
    if(layer.ezoomlayer_contribs.find(_.part_id.exists(_ == member.assignedPart)).isEmpty){
      val partId = member.assignedPart
      Logger.debug("Creating assigned part " + partId)
      val newPart = EzlPart("part:"+UUID.randomUUID, layer.ezoomlayer_id, layer.ezoombook_id,
        member.userId, Some(partId), books.dal.Status.workInProgress, false, bookPart.title.getOrElse(""),
        None, List[AtomicContrib]())
      val newLayer = EzoomLayer.contribsL.set(layer, layer.ezoomlayer_contribs :+ newPart)
      BookDO.saveLayer(newLayer)
      Some(newLayer)
    } else None
  }

  private def getBookPart(book:Book, partId:String):Option[BookPart] = {
    book.bookParts.find(_.partId == partId)
  }

  /**
   * Loads an ezoomlayer from a marked down file and displays it
   * in the ezoomlayer edition form
   */
  def loadEzoomLayer(ezbId: String, projId: String) = Action(parse.multipartFormData) {
    implicit request =>
      withUser {
        user =>
          withEzoomBook(ezbId) {
            ezb =>
              val ezlform = request.body.file("ezlfile").map {
                filePart =>
                  withFallBack(scala.io.Source.fromFile(filePart.ref.file)(_))
                    .map(_.getLines.toSeq)
                    .map(books.util.Transformer(_))
                    .map(
                      _.right.flatMap(toIndexedContribs(_).asEither).fold(
                        error => {
                          Logger.error("Unable to parse ezLayer: " + error)
                          ezoomlayerForm.withGlobalError("An error occurred while trying to load the file.")
                        },
                        layerData => {
                          Logger.debug("Layer data: " + layerData)
                          ezoomlayerForm(ezb.ezoombook_id, UUID.randomUUID, user.id).bind(layerData)
                        }
                      )
                    ) match {
                    case Failure(err) =>
                      Logger.error("An error occurred while reading the file")
                      ezoomlayerForm.withGlobalError(Messages("ezoomlayeredit.loadfile.err.invalidformat"))
                    case Success(form) => form
                  }
              }.getOrElse {
                Logger.error("Could not load eZoomLayer from file: no file found in the request")
                ezoomlayerForm.withGlobalError(Messages("ezoomlayeredit.loadfile.err.nofile"))
              }
              val projOpt = projId.toUUID.fold(
                err => None ,
                pid => BookDO.getProject(pid)
              )
              Ok(views.html.ezoomlayeredit(ezb,
                None,
                ezlform,
                BookDO.getBook(ezb.book_id.toString),
                projOpt,
                canEditEzb(ezbId) _,
                canEditProjContrib(ezb, None, projOpt) _))
          }
      }
  }

  import play.api.libs.json._
  import play.api.libs.json.Reads._
  import play.api.data.validation.ValidationError
  import play.api.libs.functional.syntax._

  def jsArrayReads[A <: JsValue](vreads:Reads[A]):Reads[JsArray] = new Reads[JsArray]{
    def reads(jsvalue:JsValue) = jsvalue match{
      case JsArray(ts) =>
        list(vreads).reads(jsvalue).map(lst =>
          JsArray(lst.toSeq)
        )
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsarray"))))
    }
  }

  private def toIndexedContribs(jsonIn:JsValue):JsResult[JsObject] = {
    var i = 0
    var j = 0

    def contribIndex:String = {
      val result = i.toString
      i += 1
      result
    }

    def partContribIndex:String = {
      val result = j.toString
      j += 1
      result
    }

    def addIndexes(ci: => String, pci: => String) = (
      (__ \ 'ezoomlayer_contribs).json.update(
         __.read[JsArray](jsArrayReads(
              __.json.update(__.read[JsObject].map{o => o ++ Json.obj("contrib_index" -> ci)}) andThen
              (__ \'part_contribs).json.update(
                __.read[JsArray](jsArrayReads(
                  (__ \ 'contrib_index).json.put(JsString(pci)) and
                  (__ \ 'contrib).json.copyFrom[JsObject]( __.json.pick[JsObject] ) reduce
                ))
              )
         ))
      )
    )

    jsonIn.transform(addIndexes(contribIndex, partContribIndex))
  }

  /**
   * Try 2 most used encodings, and fail if none is possible
   * @param file
   * @return
   */
  private def withFallBack(f: io.Codec => io.Source): Try[io.Source] = {
    Try {
      Logger.debug(s"Reading file with encoding ${io.Codec.UTF8} ...")
      f(io.Codec.UTF8)
    }.recoverWith {
      case err =>
        Logger.debug(s"Failed reading file as UTF8, trying ${io.Codec.ISO8859} enconding...")
        Try(f(io.Codec.ISO8859))
    }
  }

  //TODO rename to simply book
  def readBook(id: String) = StackAction(AuthorityKey -> Guest) {
    implicit request =>
      BookDO.getBook(id).map {
        book =>
          val ezb = Ezoombook(UUID.randomUUID, UUID.fromString(id), context.user.get.id.toString, books.dal.Status.workInProgress, "", false)
          Ok(views.html.book(book, ezoomBookForm.fill(ezb), BookDO.getEzoomBooks(UUID.fromString(id))))
      }.getOrElse {
        println("[ERROR] Could not load book " + id)
        BadRequest(views.html.listbooks(BookDO.listBooks, bookForm))
      }
  }

  def bookCover(bookId: String) = StackAction(AuthorityKey -> Guest) {
    implicit request =>
      val cover = BookDO.getBookCover(UUID.fromString(bookId))
      //    val fis = new FileInputStream(new File("/Users/mayleen/ezoombook2.png"))
      //    val cover = IOUtils.toByteArray(fis)
      if (cover.size > 0) {
        Ok(cover).as(play.api.libs.MimeTypes.forExtension("png").getOrElse(play.api.http.MimeTypes.BINARY))
      } else {
        Redirect(routes.Assets.at("/images/bookcover.png"))
      }
  }

  def cachedBookCover = StackAction(AuthorityKey -> Guest) {
    implicit request =>
      getCachedBook.map {
        cb =>
          if (!cb.bookCover.isEmpty)
            Ok(cb.bookCover).as(play.api.libs.MimeTypes.forExtension("png").getOrElse(play.api.http.MimeTypes.BINARY))
          else
            Redirect(routes.Assets.at("/images/bookcover.png"))
      }.getOrElse {
        Redirect(routes.Assets.at("/images/bookcover.png"))
      }
  }

  /**
   * Gets the current working book from the cache
   * @return
   */
  private def getCachedBook: Option[Book] = {
    Cache.getAs[Book]("ebook")
  }

  def listbooks = StackAction(AuthorityKey -> Guest) {
    implicit request =>
      Ok(views.html.listbooks(BookDO.listBooks, bookForm))
  }

  def reedit(id: String) = StackAction(AuthorityKey -> RegisteredUser) {
    implicit request =>
      BookDO.getBook(id).map {
        b =>
          Cache.set("ebook", b, 0)
          Ok(views.html.bookreedit(List[(String, Long)](), bookForm.fill(b)))
      }.getOrElse {
        println("[ERROR] ")
        BadRequest(views.html.bookreedit(List[(String, Long)](), bookForm.withGlobalError("An error occured")))
      }
  }

  def setReadingEzb(bookId: String, ezbId: String, layer: String) = StackAction(AuthorityKey -> Guest) {
    implicit request =>
      BookDO.getBook(bookId).map {
        book =>
          Redirect(routes.EzoomBooks.readEzb(book.bookId.toString, book.bookParts(0).partId)).withSession(
            session + ("working-ezb" -> ezbId) + ("show-layer" -> layer)
          )
      }.getOrElse {
        NotFound("Oops! We couldn't find the eZoomBook you are looking for")
      }
  }

  def setReadingEzbPart(bookId: String, ezbId: String, part: String, layer: String) = StackAction(AuthorityKey -> Guest) {
    implicit request =>
      Redirect(routes.EzoomBooks.readEzb(bookId, part)).withSession(
        session + ("working-ezb" -> ezbId) + ("show-layer" -> layer)
      )
  }

  def readEzb(bookId: String, partId: String) = StackAction(AuthorityKey -> Guest) {
    implicit request =>
      BookDO.getBook(bookId).flatMap {
        book =>
          context.activeEzb.flatMap {
            ezbId =>
              BookDO.getEzoomBook(ezbId).map {
                ezb =>
                  val partIndex = book.bookParts.indexWhere(_.partId == partId)
                  val (styles, bodyContent) = BookDO.getPartContentAndStyle(book.bookId, partId)
                  val level = session.get("show-layer").getOrElse("0")
                  val layers = ezb.ezoombook_layers.foldLeft(Map[String, EzoomLayer]()) {
                    (lst, layer) =>
                      BookDO.getEzoomLayer(UUID.fromString(layer._2)).
                        map(ezl => lst + (layer._1 -> ezl)).getOrElse(lst)
                  }

                  val selectedQuote:Option[AtomicContrib] = (for{
                    layerId <- session.get("from-layer")
                    quoteId <- session.get("selected-quote")
                    l <- layers.find(ll => ll._2.ezoomlayer_id.toString == layerId).map(_._2)
                    part <- l.ezoomlayer_contribs.find(_.part_id.exists(_ == partId)).collect{case p:EzlPart => p}
                    quote <- part.part_contribs.find(_.contrib_id == quoteId)
                  } yield (quote)).collect{
                      case ac:AtomicContrib => ac
                    }
                  Ok(views.html.read(book,
                    ezb, partIndex, layers,
                    play.api.templates.Html(bodyContent),
                    play.api.templates.Html(""),
                    level,
                    selectedQuote))
              }
          }
      }.getOrElse {
        NotFound("Oops! We couldn't find the resource you are looking for")
      }
  }

  def goToPart(bookId: String, partId: String) = StackAction(AuthorityKey -> Guest) {
    implicit request =>
      Redirect(routes.EzoomBooks.readEzb(bookId, partId)).withSession(
        session + ("show-layer" -> "0")
      )
  }

  def zoomIn(bookId:String, fromLayer:String, partId:String, contribId:String) = StackAction(AuthorityKey -> Guest){
    implicit request =>
      Redirect(routes.EzoomBooks.readEzb(bookId, partId)).withSession(
        session + ("selected-quote" -> contribId) + ("show-layer" -> "0") + ("from-layer" -> fromLayer)
      )
  }

  def bookResource(bookId: String, file: String) = StackAction(AuthorityKey -> Guest) {
    implicit request =>
      if (file.contains(".html")) {
        Redirect(routes.EzoomBooks.readEzb(bookId, file))
      } else {
        val mtype = file.split('.').lastOption match {
          case Some(ext) => play.api.libs.MimeTypes.forExtension(ext).getOrElse(play.api.http.MimeTypes.BINARY)
          case _ => play.api.http.MimeTypes.BINARY
        }
        val res = BookDO.getBookResource(UUID.fromString(bookId), file)

        if (res.size > 0) {
          Ok(res).as(mtype)
        } else {
          Redirect(routes.Assets.at("/images/bookcover.png"))
        }
      }
  }

  def readLayer(bookId: String, partId: String) = StackAction(AuthorityKey -> Guest) {
    implicit request =>
      readLayerAction(None, bookId, partId)
  }

  def readProjLayer(projId:String, bookId: String, partId: String) = StackAction(AuthorityKey -> Guest) {
    implicit request =>
      readLayerAction(Some(projId), bookId, partId)
  }

  private def readLayerAction[A](projId:Option[String], bookId: String, partId: String)(implicit request:Request[A]) = {
      BookDO.getBook(bookId).map {
        book =>
          val partIndex = book.bookParts.indexWhere(_.partId == partId)
          val ezoomlayeropt = context.activeLayer.flatMap(BookDO.getEzoomLayer(_))

          val quoteRanges: List[String] = ezoomlayeropt.flatMap {
            ezl =>
              ezl.ezoomlayer_contribs.find(_.part_id == Some(partId)).map {
                case part: EzlPart =>
                  for (pc <- part.part_contribs if pc.contrib_type == "contrib.Quote") yield {
                    pc.range.getOrElse("")
                  }
              }
          }.getOrElse {
            List[String]()
          }

          val (styles, bodyContent) = BookDO.getPartContentAndStyle(book.bookId, partId)

          Ok(views.html.bookread(book, projId, ezoomlayeropt,
            partIndex, partId, quoteRanges,
            play.api.templates.Html(bodyContent),
            play.api.templates.Html("")))
      }.getOrElse {
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
    )((uid, bookId, ezbId, layerId, partId, content, range) =>
    AtomicContrib("quote:" + UUID.randomUUID, "contrib.Quote", UUID.fromString(layerId), UUID.fromString(ezbId),
      UUID.fromString(uid), partId, range, books.dal.Status.workInProgress, false, content))

  /**
   * Receives a Json document containing a quote and adds it to the database
   * TODO Add authorizied access restriction
   * @return
   */
  def addQuote = Action(parse.json) {
    request =>
      request.body.validate[AtomicContrib].map {
        case contrib =>
          BookDO.getEzoomLayer(contrib.ezoomlayer_id).flatMap{
            ezl =>
              val result:Option[play.api.mvc.Result] =
              (for {
                ezb <- BookDO.getEzoomBook(ezl.ezoombook_id)
                book <- BookDO.getBook(ezb.book_id.toString)
                part <- book.bookParts.find(_.partId == contrib.part_id.get)
                pt <- part.title
              } yield ( pt )) map { partTitle =>
                  val modifedLayer = EzoomLayer.updatePart(ezl, contrib.part_id.getOrElse(""), partTitle, contrib)
                  BookDO.saveLayer(modifedLayer)
                  Ok("Quote saved!")
              }
              result
          }.getOrElse{
            Logger.error("An error occurred while saving quote: " + contrib)
            BadRequest("An error occurred while saving quote.")
          }
      }.recoverTotal {
        e =>
          Logger.error("Json error detected while saving quote: " + JsError.toFlatJson(e))
          BadRequest("Detected error:" + JsError.toFlatJson(e))
        }
      }

  def ezoomLayerDelete(ezbId: String, layerLevel: Int) = StackAction(AuthorityKey -> RegisteredUser) {
    implicit request =>
      BookDO.deleteEzoomLayer(UUID.fromString(ezbId), layerLevel)
      Redirect(routes.EzoomBooks.ezoomBookEdit(ezbId))
  }

  def ezoomBookDelete(ezbId: String) = StackAction(AuthorityKey -> RegisteredUser) {
    implicit request =>
      withUser {
        user =>
        //TODO Validate that user has the right to delete ezb
          BookDO.deleteEzoomBook(UUID.fromString(ezbId))
          Redirect(routes.Workspace.home)
      }
  }

  def getContribution(layerId: String, contribId: String) = StackAction(AuthorityKey -> Guest) {
    request =>
      BookDO.getEzoomLayer(UUID.fromString(layerId)).flatMap {
        layer =>
          layer.ezoomlayer_contribs.collectFirst {
            case atomic: AtomicContrib if (atomic.contrib_id == contribId) => atomic
            case part: EzlPart if (part.part_contribs.exists(_.contrib_id == contribId)) =>
              part.part_contribs.find(_.contrib_id == contribId).get
          }.map {
            contrib =>
              Ok(Json.toJson(contrib.copy(contrib_type = Messages("application." + contrib.contrib_type))))
          }
      }.getOrElse {
        NotFound(s"Contribution $layerId:$contribId not found")
      }
  }

  def withEzoomBook(ezbId: String)(block: (Ezoombook) => Result): Result = {
    ezbId.toUUID.fold(
      err => BadRequest("Oh snap! Your eZoomBook's identifier is not a valid id :("),
      eid => BookDO.getEzoomBook(eid).map {
        ezb => block(ezb)
    }.getOrElse {
      NotFound("Oops! We couldn't find the EzoomBook you are looking for :(")
      }
    )
  }

  def withUserAndEzb[A](request: Request[A], ezbId: String)(block: (User, Ezoombook) => Result): Result = {
    implicit val req = request
    withUser {
      user =>
        withEzoomBook(ezbId) {
          ezb =>
            block(user, ezb)
        }
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
