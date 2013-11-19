package models

import books.dal._
import books.util._
import ezb.comments._
import project.dal.{EzbProject,TeamMember}
import utils.xml.Helper._

import java.util.UUID
import java.io.{InputStream, FileInputStream, ByteArrayInputStream, File}
import java.util.zip.ZipFile
import play.api.cache.Cache

import play.api.Play.current

object BookDO{

  import AppDB._
  import AppDB.dal.profile.simple._

  def newBook(byteArray: Array[Byte]):Book = EpubLoader.loadBook(Left(new ByteArrayInputStream(byteArray)))

  def newBook(file: File):Book = {
    val ziped = new ZipFile(file)
    EpubLoader.loadBook(Right(ziped))
  }

  def saveBook(book:Book){
    Cache.getAs[ZipFile]("epub").map{epub =>
      AppDB.cdal.saveBookResources(book.bookId, epub)
      AppDB.cdal.saveBook(book)
    }.getOrElse{
      println("[ERROR] Could not save resources for loaded book. Cached epub not found")
    }
  }

  def saveEzoomBook(ezb:Ezoombook){
    AppDB.cdal.saveEzoomBook(ezb)
  }

  def saveLayer(ezl:EzoomLayer){
    AppDB.cdal.saveLayer(ezl)
  }

  /**
   * Returns the list of eZoomBooks of a Book
   * @param bookId the id of the Book
   * @return
   */
  def getEzoomBooks(bookId:UUID):List[Ezoombook] = {
    AppDB.cdal.getEzoomBooks(bookId)
  }

  /**
   * Returns the eZoomBook with the given ID
   * @param ezbId
   * @return
   */
  def getEzoomBook(ezbId:UUID):Option[Ezoombook] = {
      AppDB.cdal.getEzoomBook(ezbId)
  }

  def getEzoomLayer(ezlId:UUID, reloadCache:Boolean = false):Option[EzoomLayer] = {
    AppDB.cdal.getLayer(ezlId)
  }

  def deleteEzoomLayer(ezbId:UUID, layerLevel:Int){
    AppDB.cdal.deleteLayer(ezbId, layerLevel)
  }

  def deleteEzoomBook(ezbId:UUID){
    AppDB.cdal.deleteEzb(ezbId)
  }

  def listBooks():List[Book] = {
    AppDB.cdal.listBooks()
  }

  def getBook(bookId:String):Option[Book] = {
    AppDB.cdal.getBook(UUID.fromString(bookId))
  }

  /**
   * Creates a new project
   */
  def saveProject(proj:EzbProject){
    AppDB.cdal.saveProject(proj)
  }

  /**
   * Returns the projects owned by a user
   */
  def getOwnedProjects(uid:UUID):List[EzbProject] = {
    AppDB.cdal.getProjectsByOwner(uid)
  }

  /**
   * Returns the projects of a group
   */
  def getGroupProjects(pid:UUID):List[EzbProject] = {
    AppDB.cdal.getProjectsByGroup(pid)
  }

  /**
   * Returns the project with id projId
   */
  def getProject(projId:UUID):Option[EzbProject] = {
    AppDB.cdal.getProjectById(projId)
  }

  def addProjectMember(projId:UUID,member:TeamMember):Option[EzbProject] = {
    AppDB.cdal.addProjectMember(projId,member)
  }

  def updateProjectMember(projId:UUID, member:TeamMember):Option[EzbProject] = {
    AppDB.cdal.updateProjectMember(projId,member)
  }

  def updateProjectEzb(projId:UUID, projEzb:UUID):Option[EzbProject] = {
    AppDB.cdal.updateProjectEzb(projId, projEzb)
  }

  def deleteProject(projId:UUID){
    AppDB.cdal.deleteProject(projId)
  }

  def getBookCover(bookId:UUID):Array[Byte] = {
    AppDB.cdal.getBookCover(bookId)
  }

  def getBookResource(bookId:UUID,resPath:String):Array[Byte] = {
    AppDB.cdal.getBookResource(bookId,resPath)
  }

  /**
   * Returns the styles and content of a book part excluding the header and body components
   * @param bookId
   * @param resPath
   * @return The tuple (styles, bodyContent)
   */
  def getPartContentAndStyle(bookId:UUID,partPath:String):(String,String) = {
    import xml.XML
    import java.io.ByteArrayInputStream

    //Actual resource path
    val resPath = partPath.split('#')(0)
    val contentRaw = getBookResource(bookId, resPath)
    if (contentRaw.isEmpty){
      println("[ERROR] Resource " + partPath + " not found!")
      ("","")
    }else{
      val content = xml.parsing.XhtmlParser(io.Source.fromBytes(contentRaw))
      //Transform paths
      val newContent = transform(content.head){
        case node:scala.xml.Elem =>
          node.copy(attributes = mapMetaData(node.attributes){
            case g @ GenAttr(_, key, value, _) if (key == "src" || key == "href") =>
              g.copy(value = scala.xml.Text("/bookres/"+bookId+"/"+relative2absolute(value.mkString)))
            case other => other
          })
        case other => other
      }
      //Get body
      val bodyContent = xml.NodeSeq.fromSeq((newContent \ "body").head.child).mkString
      //Get styles
      val styles = (newContent \\ "link").mkString
      //-- end of TODO

      (styles, bodyContent)
    }
  }

  //Transform a path begining with "../" into a path begining with "/"
  private def relative2absolute(path:String):String = {
    path.replaceAllLiterally    ("../", "")
  }

  /**
   * Returns the list of books uploaded by a user
   * @param uid
   */
  def getUserBooks(uid:UUID):List[Book] = {
    AppDB.database.withSession{
      implicit session:Session =>
        AppDB.dal.UserBooks.getBooksByUser(uid).foldLeft(List[Book]()){(lst,ubook) =>
          AppDB.cdal.getBook(ubook._1).map{book =>
            lst :+ book
          }.getOrElse{
            println("[ERROR] Could not find book with id " + ubook._1)
            lst
          }
        }
    }
  }

  /**
   * Resturns the eZoomBooks created by the user
   */
  def getUserEzoombooks(uid:UUID):List[Ezoombook] = {
    AppDB.cdal.getEzoombooksUser(uid)
  }

  /**
   * Returns the project where the user participates
   */
  def getProjectsByMember(uid:UUID):List[EzbProject] = {
    AppDB.cdal.getProjectsByMember(uid)
  }

  def saveComment(comment:Comment) = {
    AppDB.cdal.saveComment(comment)
  }

  def getCommetsByEzb(ezbId:UUID):List[Comment] = {
    AppDB.cdal.commentsByEzb(ezbId)
  }

  def changeEzbOwner(ezbid:UUID, newOwner:String) = {
    AppDB.cdal.getEzoomBook(ezbid).map{ ezb =>
      val newEzb = ezb.copy(ezoombook_owner = newOwner)
      AppDB.cdal.saveEzoomBook(newEzb)
      newEzb
    }
  }

  def changeLayerOwner(ezbLayer:UUID, newOwner:String) = {
    AppDB.cdal.getLayer(ezbLayer).map{layer =>
      val newLayer = layer.copy(ezoomlayer_owner = newOwner)
      AppDB.cdal.saveLayer(newLayer)
      newLayer
    }
  }
}
