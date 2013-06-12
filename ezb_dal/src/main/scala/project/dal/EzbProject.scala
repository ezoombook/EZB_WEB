package project.dal

import books.dal.Book
import users.dal.User
import books.util.UUIDjsParser

import scala.collection.JavaConversions._
import play.api.libs.json._
import play.api.libs.functional._

import java.util.UUID
import com.couchbase.client.CouchbaseClient
import com.couchbase.client.protocol.views.Query

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 11/06/13
 * Time: 15:23
 * To change this template use File | Settings | File Templates.
 */
case class EzbProject(projectId:UUID, projectName:String, projectOwnerId:UUID, projectCreationDate:Long,
                      groupId:UUID, ezoombookId:UUID, projectTeam:List[TeamMember])

case class TeamMember(userId:UUID, assignedPart:String, assignedLayer:UUID)

object EzbProject extends UUIDjsParser{
  implicit val mfmt = Json.format[TeamMember]

  implicit val fmt = Json.format[EzbProject]
}

trait EzbProjectComponent{
  def saveProject(ezbProject:EzbProject)(implicit couchclient:CouchbaseClient){
    val key = "project:"+ezbProject.projectId.toString
    couchclient.set(key, 0, Json.toJson(ezbProject).toString)
  }

  def getProjectById(projId:UUID)(implicit couchclient:CouchbaseClient):Option[EzbProject] = {
    couchclient.get("project:"+projId) match{
      case str:String =>
        Json.parse(str).validate[EzbProject].fold(
          err => {
            println("[ERROR] Could not parse Json to EzbProject: " + err)
            None
          },
          ezbProj => Some(ezbProj)
        )
      case _ =>
        println("[ERROR] Could not find EzbProject with id " + projId.toString)
        None
    }
  }

  def getProjectsByOwner(userId:UUID)(implicit couchclient:CouchbaseClient):List[EzbProject] = {
    val view = couchclient.getView("projects","by_owner")
    val query = new Query()

    query.setIncludeDocs(true).setKey(userId.toString)
    couchclient.query(view,query).foldLeft(List[EzbProject]()){(lst,row) =>
      val js = Json.parse(row.getDocument().asInstanceOf[String])
      js.validate[EzbProject].fold(
        err => {
          println("[ERROR] Found invalid Json document " + err)
          lst
        },
        ezbProj => ezbProj +: lst
      )
    }.toList
  }

//  def getProjectsByMember(implicit couchclient:CouchbaseClient):List[EzbProject] = {
//
//  }
//
//  def addMember(uid:UUID, assignedPart:String, assignedLayer:UUID){
//
//  }
//
//  def removeMember(memId:UUID)(implicit couchclient:CouchbaseClient){
//
//  }
//
//  def deleteProject(pId:UUID)(implicit couchclient:CouchbaseClient){
//
//  }
}

