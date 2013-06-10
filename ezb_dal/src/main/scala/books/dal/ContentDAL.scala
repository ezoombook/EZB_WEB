package books.dal

import java.util.concurrent.TimeUnit
import com.couchbase.client.CouchbaseClient

class ContentDAL(val couchclient:CouchbaseClient) extends BookComponent{
  def disconnect(){
    couchclient.shutdown(5, TimeUnit.SECONDS)
  }

}
