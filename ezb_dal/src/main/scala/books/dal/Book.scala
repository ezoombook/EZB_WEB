package books.dal

import books.util.UUIDjsParser
import play.api.libs.json._
import play.api.libs.functional._
import java.util.UUID
import com.couchbase.client.CouchbaseClient

case class Book (bookId:UUID, bookTitle:String, bookAuthors:List[String], bookLanguages:List[String], 
		 bookPublishers:List[String], bookPublishedDates:List[String], bookTags:List[String],
	         bookSummary:String, bookParts:List[BookPart]){

  def this(bookTitle:String, bookAuthors:List[String], bookLanguages:List[String], 
		 bookPublishers:List[String], bookPublishedDates:List[String], bookTags:List[String],
	         bookSummary:String, bookParts:List[BookPart]) = this(UUID.randomUUID, bookTitle, bookAuthors, bookLanguages, 
		 bookPublishers, bookPublishedDates, bookTags,
	         bookSummary, bookParts)

  override def toString = "{id = "+bookId+", title=" + bookTitle+ ", authors= "+bookAuthors.mkString("[",",","]") +
                          ", publishers= " + bookPublishers.mkString("[",",","]") +
                          ", published dates= " + bookPublishedDates.mkString("[",",","]") +
                          ", tags= " + bookTags.mkString("[",",","]") +
                          ", summary= " + bookSummary
}

case class BookPart(val partId:String, val bookId:UUID, val content:Array[Byte]){}

object Book extends UUIDjsParser{

  implicit val BookPartWrites:Writes[BookPart] = new Writes[BookPart]{
    def writes(b:BookPart) = JsString(b.partId) 
  }

  implicit val BookPartReads:Reads[BookPart] = new Reads[BookPart]{
    def reads(jval: JsValue) = jval match{
      case JsString(s) => JsSuccess(new BookPart(s, null, null))
      case _ => JsError("Expected: id. Found: " + jval)
    }
  }

  implicit val fmt = Json.format[Book]
}

trait BookComponent{
  def couchclient:CouchbaseClient

  def addBook(book:Book){
    val key = "book:"+book.bookId
    couchclient.set(key, 0, Json.toJson(book).toString)
    for(bp <- book.bookParts){
      addPart(bp)
    }
  }

  def addPart(part:BookPart){
    couchclient.set("part:"+part.partId, 0, part.content) 
  }

  def saveEzoomBook(ezb:Ezoombook){

  }

  def saveLayer(ezl:EzoomLayer){
    val key = "ezoomlayer:"+ezl.ezoomlayer_id
    couchclient.set(key, 0, Json.toJson(ezl).toString())
  }
}
