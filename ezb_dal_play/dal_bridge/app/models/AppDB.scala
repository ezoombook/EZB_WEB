package models
import play.api.Play.current

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
  def storeTemporalLinkId(linkId:String, userId:String){
    cdal.couchclient.set("link:"+linkId, 60*60*42, userId)
  }

  def getTemporalLinkId(id:String):Option[String] = {
    cdal.couchclient.get("link:"+id) match{
      case str:String =>
        cdal.couchclient.delete("link:"+id)
        Some(str)
      case _ => None
    }
  }
}
