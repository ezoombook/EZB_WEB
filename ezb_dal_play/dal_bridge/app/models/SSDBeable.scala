package models

import books.dal.ContentDAL

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

  def getContentDal(implicit app : Application):ContentDAL = {
    val uris = ArrayBuffer(URI.create(app.configuration.getString(COUCHBASE_URL).getOrElse("http://localhost/pools:8091")))
    val bucket = app.configuration.getString(COUCHBASE_BUCKET).getOrElse("default")
    val password = app.configuration.getString(COUCHBASE_PASSWORD).getOrElse("")

    val client = new CouchbaseClient(uris, bucket, password)

    new ContentDAL(client)
  }

}
