package models

import ezb.ContentDAL

import collection.{JavaConversions,mutable}
import JavaConversions._
import play.api.Application
import mutable.ArrayBuffer
import java.net.URI
import com.couchbase.client.CouchbaseClient

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 11/02/13
 * Time: 12:03
 * To change this template use File | Settings | File Templates.
 */
trait SSDBeable {
  val COUCHBASE_URL="cb.books.url"
  val COUCHBASE_BUCKET="cb.books.bucket"
  val COUCHBASE_PASSWORD="cb.books.password"

  import play.api.Play.current

  implicit val couchbaseClient = getCouchabaseClient

  def getCouchabaseClient(implicit app: Application):CouchbaseClient = {
    val uris = ArrayBuffer(URI.create(app.configuration.getString(COUCHBASE_URL).getOrElse("http://localhost:8091/pools")))
    val bucket = app.configuration.getString(COUCHBASE_BUCKET).getOrElse("default")
    val password = app.configuration.getString(COUCHBASE_PASSWORD).getOrElse("")

    new CouchbaseClient(uris, bucket, password)
  }

  def getContentDal:ContentDAL = {
    new ContentDAL(getCouchabaseClient)
  }

}
