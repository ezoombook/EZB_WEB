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
object Global extends DalSettings{

/*
  override def onStart(app: Application) {
    implicit val application = app
    lazy val database = getDb
    lazy val dal = getDal
    database.withSession {
      implicit session: Session =>
        dal.create
	println("[INFO] Database Created!")
    }
  }
*/
}
