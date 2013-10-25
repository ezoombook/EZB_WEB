package ezb

import project.dal.EzbProjectComponent
import books.dal.BookComponent

import java.util.concurrent.TimeUnit
import com.couchbase.client.CouchbaseClient

class ContentDAL(couchclient:CouchbaseClient) extends BookComponent
  with EzbProjectComponent with comments.CommentComponent{

  implicit val client = couchclient

  def disconnect(){
    client.shutdown(5, TimeUnit.SECONDS)
  }
}
