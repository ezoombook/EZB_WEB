package project.dal

import books.dal.Book
import users.dal.User
import books.util.UUIDjsParser

import scala.collection.JavaConversions._
import play.api.libs.json._
import play.api.libs.functional._

import java.util.UUID
import com.couchbase.client.CouchbaseClient
import com.couchbase.client.protocol.views.{ViewResponse,Query}
import net.spy.memcached.CASValue

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 11/06/13
 * Time: 15:23
 * To change this template use File | Settings | File Templates.
 */
case class EzbProject(projectId:UUID,
                      projectName:String,
                      projectOwnerId:UUID,
                      projectCreationDate:Long,
                      groupId:UUID,
                      ezoombookId:UUID,
                      projectTeam:List[TeamMember]){
}

case class TeamMember(userId:UUID, assignedPart:String, assignedLayer:UUID)

object EzbProject extends UUIDjsParser{
  implicit val mfmt = Json.format[TeamMember]

  implicit val fmt = Json.format[EzbProject]
}

trait EzbProjectComponent{
  private def projectKey(pid:UUID):String = "project:"+pid

  def saveProject(ezbProject:EzbProject)(implicit couchclient:CouchbaseClient){
    val key = projectKey(ezbProject.projectId)
    couchclient.set(key, 0, Json.toJson(ezbProject).toString)
  }

  def getProjectById(projId:UUID)(implicit couchclient:CouchbaseClient):Option[EzbProject] = {
    couchclient.get(projectKey(projId)) match{
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
    parseProjectResult(couchclient.query(view,query))
  }

  def getProjectsByGroup(gid:UUID)(implicit couchclient:CouchbaseClient):List[EzbProject] = {
    val view = couchclient.getView("projects","by_group")
    val query = new Query()

    query.setIncludeDocs(true).setKey(gid.toString)
    parseProjectResult(couchclient.query(view,query))
  }

  private def parseProjectResult(resp:ViewResponse):List[EzbProject] = {
    resp.foldLeft(List[EzbProject]()){(lst,row) =>
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

  def getProjectsByMember(userId:UUID)(implicit couchclient:CouchbaseClient):List[EzbProject] = {
    val view = couchclient.getView("projects", "by_member")
    val query = new Query()
    query.setIncludeDocs(true).setKey(userId.toString)
    parseProjectResult(couchclient.query(view,query))
  }

  def addProjectMember(projId:UUID, newMember:TeamMember)(implicit couchclient:CouchbaseClient):Option[EzbProject] = {
    val key = projectKey(projId)
    couchclient.getAndLock(key,15) match{
      case cas:CASValue[_] =>
        Json.parse(cas.getValue().asInstanceOf[String]).validate[EzbProject].fold(
          err => {
            println("[ERROR] Found invalid Json document " + err)
            None
          },
          proj => {
            val newEzbProj = EzbProject(projId,proj.projectName,proj.projectOwnerId,proj.projectCreationDate,
              proj.groupId,proj.ezoombookId,proj.projectTeam :+ newMember)
            couchclient.cas(key, cas.getCas, Json.toJson(newEzbProj).toString)
            Some(newEzbProj)
          }
        )
      case _ =>
        println(s"[DAL-ERROR] Could not retrieve project $projId")
        None
    }
  }

//  def removeMember(memId:UUID)(implicit couchclient:CouchbaseClient){
//
//  }
//
//  def deleteProject(pId:UUID)(implicit couchclient:CouchbaseClient){
//
//  }
}

