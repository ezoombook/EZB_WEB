package models

import books.dal._
import books.util._
import project.dal.{EzbProject,TeamMember}

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
    Cache.set("epub",ziped,0)
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

//  def saveBookParts(book:Book){
//    for(bp <- book.bookParts){
//      AppDB.cdal.saveBookPart(bp)
//    }
//  }

  def setWorkingEzb(ezb:Ezoombook){
    Cache.set("working-ezb", ezb, 0)
  }

  def setWorkingLayer(ezl:EzoomLayer){
    Cache.set("working-layer", ezl, 0)
  }

  def saveEzoomBook(ezb:Ezoombook){
    AppDB.cdal.saveEzoomBook(ezb)
    Cache.set("ezb:"+ezb.ezoombook_id,ezb)
  }

  def saveLayer(ezl:EzoomLayer){
    Cache.set("ezl:"+ezl, ezl)
    setWorkingLayer(ezl)
    AppDB.cdal.saveLayer(ezl).map{ezb =>
      Cache.set("ezb:"+ezb.ezoombook_id,ezb)
      setWorkingEzb(ezb)
    }
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
    Cache.getOrElse("ezb:"+ezbId, 0){
      AppDB.cdal.getEzoomBook(ezbId)
    }
  }

  def getEzoomLayer(ezlId:UUID):Option[EzoomLayer] = {
    Cache.getOrElse("ezl:"+ezlId, 0){
      AppDB.cdal.getLayer(ezlId)
    }
  }

  def listBooks():List[Book] = {
    AppDB.cdal.listBooks()
  }

  def getBook(bookId:String):Option[Book] = {
    AppDB.cdal.getBook(UUID.fromString(bookId))
  }

//  def getBookPart(bookId:UUID,partId:String):Option[BookPart] = {
//    AppDB.cdal.getBookPart(bookId,partId)
//  }

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
    AppDB.cdal.addProjectMember(projId,member).map{proj =>
      Cache.set("project:"+projId,proj)
      proj
    }
  }

  def getBookCover(bookId:UUID):Array[Byte] = {
    AppDB.cdal.getBookCover(bookId)
  }

  def getBookResource(bookId:UUID,resPath:String):Array[Byte] = {
    Cache.getOrElse(bookId+":"+resPath)(AppDB.cdal.getBookResource(bookId,resPath))
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
}
