package models
import play.api.Play.current
import com.couchbase.client.CouchbaseClient

/**
 * Created with IntelliJ IDEA.
 * User: gonto
 * Date: 11/23/12
 * Time: 10:14 PM
 * To change this template use File | Settings | File Templates.
 */
object AppDB extends DBeable with SSDBeable{

  def database = getDb
  lazy val dal = getDal
  lazy val cdal = getContentDal

  /**
   * Links have a validity time of 42 hours
   * @param id
   */
  def storeTemporalLinkId(linkId:String, userId:String)(implicit couchclient:CouchbaseClient){
    couchclient.set("link:"+linkId, 60*60*42, userId)
  }

  def getTemporalLinkId(id:String)(implicit couchclient:CouchbaseClient):Option[String] = {
    couchclient.get("link:"+id) match{
      case str:String =>
        couchclient.delete("link:"+id)
        Some(str)
      case _ => None
    }
  }
}
