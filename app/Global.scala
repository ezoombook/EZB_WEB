import models.{DBeable, AppDB, SSDBeable}

import play.api.db.DB
import play.api.GlobalSettings
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import users.dal._
import play.api.Application
import slick.session.Session
import scala.concurrent.Future

import play.api.libs.iteratee._

import org.reactivecouchbase.play.PlayCouchbase
import net.spy.memcached.ops.OperationStatus


/**
 * Created with IntelliJ IDEA.
 * User: gonto
 * Date: 11/23/12
 * Time: 9:37 PM
 * To change this template use File | Settings | File Templates.
 */

object Global extends DalSettings with DBeable with SSDBeable{

  override def onStart(app: Application) {
    implicit val application = app
    implicit val ec = PlayCouchbase.couchbaseExecutor

    lazy val database = getDb
    lazy val dal = getDal
    lazy val cdal = getContentDal

    database.withSession {
      implicit session: Session =>
        dal.create
        Logger.info("Database Created!")
    }

    if(app.configuration.getBoolean("couchbase.applyevolution").getOrElse(false)){
      Logger.debug("Applaying evolutions...")

      val loggerIter = Iteratee.foreach[OperationStatus]{ status =>
        def log(msg:String)  = if (status.isSuccess) Logger.info(msg) else Logger.error(msg)

        log(status.getMessage)
      }

      cdal.applyEvolution.run(loggerIter)
    }

  }
  
  override def onStop(app: Application) {

  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    ex.printStackTrace()
    Future.successful(InternalServerError(
      views.html.error(
        if (Play.isProd(Play.current))
          "We are experiencing technical problems. Please try again later."
        else
          ex.getMessage
        )
    ))
  }
}


