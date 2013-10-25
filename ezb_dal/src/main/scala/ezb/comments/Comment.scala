package ezb.comments

import util.EzbImplicits._
import play.api.libs.json._
import play.api.libs.functional._
import books.util.UUIDjsParser

import scala.collection.JavaConversions._
import java.util.UUID
import java.util.Date
import com.couchbase.client.CouchbaseClient
import com.couchbase.client.protocol.views.{ViewResponse,Query}

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 21/10/13
 * Time: 17:20
 * To change this template use File | Settings | File Templates.
 */
case class Comment(commentId:UUID,
                   commentAuthor:UUID,
                   ezbId:UUID,
                   layerId:UUID,
                   partId:Option[String],
                   contribId:UUID,
                   contribAuthor:UUID,
                   contribType:String,
                   commentDate:Date,
                   commentContent:String)

object Comment extends UUIDjsParser{
  implicit val mfmt = Json.format[Comment]
}

trait CommentComponent{
  private def commentKey(cid:UUID) = "comment:"+cid

  def saveComment(comment:Comment)(implicit couchclient:CouchbaseClient){
    val key = commentKey(comment.commentId)
    couchclient.set(key,0,Json.toJson(comment).toString)
  }

  def removeComment(comId:UUID)(implicit couchclient:CouchbaseClient) = {
    val key = commentKey(comId)
    couchclient.delete(key).get().booleanValue()
  }

  def commentsByEzb(ezbId:UUID)(implicit couchclient:CouchbaseClient):List[Comment] = {
    val commntView = couchclient.getView("comments","by_ezb")
    val query = new Query()

    query.setIncludeDocs(true).setKey(ezbId.toString)
    val result = couchclient.query(commntView,query)

    result.foldLeft(List[Comment]()){(lst,row) =>
      row.getDocument().map{doc =>
        val js = Json.parse(doc.asInstanceOf[String])
        js.validate[Comment].fold(
          err => {
            println(s"[ERROR] Invalid Json document with ezb_id  = ${ezbId.toString}. Expected: Comment")
            println("[ERROR] " + err)
            lst
          },
          comment => comment +: lst
        )
      }.getOrElse(lst)
    }.toList

  }

}