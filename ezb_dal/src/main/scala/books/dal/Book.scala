package books.dal

import books.util.UUIDjsParser
import play.api.libs.json._
import play.api.libs.functional._
import java.util.UUID
import scala.collection.JavaConversions._

import com.couchbase.client.CouchbaseClient
import com.couchbase.client.protocol.views.Query

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

  def saveBook(book:Book){
    val key = "book:"+book.bookId
    couchclient.set(key, 0, Json.toJson(book).toString)
  }

  def saveBookPart(part:BookPart){
    couchclient.set("part:"+part.partId, 0, part.content)
  }

  /**
   * Return a list of books sorted by popularity
   */
  def listBooks():List[Book] = {
    var bookList = List[Book]()

    //Prepare the view and query
    val bookView = couchclient.getView("book","by_title")
    val query = new Query()

    // We the full documents and only the top 20
    query.setIncludeDocs(true).setLimit(20)

    // Send the query
    val result = couchclient.query(bookView,query)

    //Retrieve the array containing the 20 books
    result.map{row =>
      val js = Json.parse(row.getValue())

      val bookId = UUID.fromString(row.getId().split(':')(1)) //TODO Correct unsafe operation
      val authors = js(0).as[List[String]]
    //TODO use this to convert to real dates
    //js.asOpt[List[String]].map(lst => lst.foreach(d => new java.util.Date(d.toLong))).getOrElse(List[java.util.Date]())
      val publishedDates = js(1).asOpt[List[String]].map(_.map(_.toString)).getOrElse(List[String]())
      val tags = js(2).as[List[String]]
      val summary = js(3).as[String]

      Book(bookId,row.getKey(),authors,Nil,Nil,publishedDates,tags,summary,Nil)
    }.toList
  }

  def getBook(bookId:UUID):Option[Book] = {
    couchclient.get("book:"+bookId) match{
      case str:String => Json.parse(str).validate[Book].fold(
        err => {
          println(s"[ERROR] Invalid Json document with id ${bookId.toString}. Expected: Book")
          println("[ERROR] " + err)
          None
        },
        book => Some(book)
      )
      case _ => None
    }
  }

  def saveEzoomBook(ezb:Ezoombook){

  }

  def saveLayer(ezl:EzoomLayer){
    val key = "ezoomlayer:"+ezl.ezoomlayer_id
    couchclient.set(key, 0, Json.toJson(ezl).toString())
  }
}
