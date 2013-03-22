/**
 * Minimal Operations for working with a key/value database
 */ 
trait KeyValueDBClient{
  /**
   * Adds a new key-value pair to the database. The key is not already present in the database.
   */ 
  def add(key:String, expiry:Int, value:Any):Future[Boolean]

  /**
   * Appends a new value to an existing key
   */ 
  def append(key:String, value:Any)

  /**
   * Deletes the item identifed with a given key
   */ 
  def delete(key:String):Future[Boolean]

  /**
   * Retrieves the item idefied with the given key
   */
  def get(key:String):Option[Any]


}
