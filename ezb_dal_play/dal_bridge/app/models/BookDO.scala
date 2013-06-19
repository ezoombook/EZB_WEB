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

  def newBook(byteArray: Array[Byte]):Book = EpubLoader.loadBook(Left(new ByteArrayInputStream(byteArray)))

  def newBook(file: File):Book = EpubLoader.loadBook(Right(new ZipFile(file)))

  def saveBook(book:Book){
    AppDB.cdal.saveBook(book)
  }

  def saveBookParts(book:Book){
    for(bp <- book.bookParts){
      AppDB.cdal.saveBookPart(bp)
    }
  }

  def saveEzoomBook(ezb:Ezoombook){
    AppDB.cdal.saveEzoomBook(ezb)
    Cache.set("ezb:"+ezb.ezoombook_id,ezb)
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
}
