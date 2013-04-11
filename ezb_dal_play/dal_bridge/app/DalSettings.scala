import models.{DBeable, AppDB}
import play.api.db.DB
import play.api.GlobalSettings

import users.dal._
import play.api.Application
import slick.session.Session


/**
 * Created with IntelliJ IDEA.
 * User: gonto
 * Date: 11/23/12
 * Time: 9:37 PM
 * To change this template use File | Settings | File Templates.
 */
trait DalSettings extends GlobalSettings{

  /**
   * Initializes the tables if they are not already present in the database
   */ 
  override def onStart(app: Application) {
    AppDB.database.withSession{
      implicit session:Session =>
	    AppDB.dal.create
    }
  }

  /**
   * Close the couchbase client session
   */
  override def onStop(app: Application) {
    AppDB.cdal.disconnect()
  }
}
