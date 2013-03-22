package books.dal

class ContentDAL(val couchClient:CouchbaseClient) extends BookComponent{
  def disconnect(){
    couchClient.shutdown()
  }
}
