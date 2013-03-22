package books.dal

import play.api.libs.json._
import play.api.libs.functional._

case class Book (bookId:UUID, bookTitle:String, bookAuthors:List[String], bookLanguages:List[String], 
		 bookPublisher:String, bookPublishedDate:String, bookTags:List[String],
	         bookSummary:String, bookParts:List[String])

object Book{
  implicit val fmt = Json.format[Book]
}

trait BookComponent{
  def couchclient:CouchbaseClient

  def addBook(book:Book){
    val key = "book:"+book.bookId
    couchClient.set(key, 0, Json.toJson(book))
  }
}
