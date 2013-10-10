package books.dal

import books.util.{UUIDjsParser, EpubLoader}
import util.EzbImplicits._

import play.api.libs.json._
import play.api.libs.functional._

import java.util.UUID
import net.spy.memcached.CASValue

import scala.collection.JavaConversions._

import com.couchbase.client.CouchbaseClient
import com.couchbase.client.protocol.views.Query
import java.util.zip.ZipFile

case class Book (bookId:UUID, bookTitle:String, bookAuthors:List[String], bookLanguages:List[String], 
		 bookPublishers:List[String], bookPublishedDates:List[String], bookTags:List[String],
	         bookSummary:String, bookCover: Array[Byte], bookParts:List[BookPart]){

  def this(bookTitle:String, bookAuthors:List[String], bookLanguages:List[String], 
		 bookPublishers:List[String], bookPublishedDates:List[String], bookTags:List[String],
	         bookSummary:String, bookParts:List[BookPart]) = this(UUID.randomUUID, bookTitle, bookAuthors, bookLanguages,
		 bookPublishers, bookPublishedDates, bookTags,
	         bookSummary, Array[Byte](), bookParts)

  def setCover(newCover:Array[Byte]):Book = {
    Book(bookId, bookTitle, bookAuthors, bookLanguages, bookPublishers, bookPublishedDates, bookTags,
	 bookSummary, newCover, bookParts) 
  }  

  override def toString = "{id = "+bookId+", title=" + bookTitle+ ", authors= "+bookAuthors.mkString("[",",","]") +
                          ", publishers= " + bookPublishers.mkString("[",",","]") +
                          ", published dates= " + bookPublishedDates.mkString("[",",","]") +
                          ", tags= " + bookTags.mkString("[",",","]") +
                          ", summary= " + bookSummary
}

/*
  A part contained in the spine of the book
  @partId Is the href path of the epub resource
 */
case class BookPart(val partId:String, val title:Option[String]){}

object Book extends UUIDjsParser{
  import play.api.libs.functional.syntax._

  implicit val bookPartFormat:Format[BookPart] = (
    (__ \ "partId").format[String] ~
    (__ \ "title").format[Option[String]]
  )((partId, title) => BookPart(partId, title),
  part => (part.partId, part.title))

  val emptyArray = new Format[Array[Byte]] {
    def writes(v:Array[Byte]) = JsArray()
    def reads(jval: JsValue) = JsSuccess(Array[Byte]())
  }

  implicit val fmt: Format[Book] = (
    (__ \ "bookId").format[UUID] ~
    (__ \ "bookTitle").format[String] ~
    (__ \ "bookAuthors").format[List[String]] ~
    (__ \ "bookLanguages").format[List[String]] ~
    (__ \ "bookPublishers").format[List[String]] ~
    (__ \ "bookPublishedDates").format[List[String]] ~
    (__ \ "bookTags").format[List[String]] ~
    (__ \ "bookSummary").format[String] ~
    (__ \ "bookParts").format[List[BookPart]]
  )((bid, title, authors, langs, publs, pubdats, tags, summ, parts) =>
      Book(bid, title, authors, langs, publs, pubdats, tags, summ, Array[Byte](), parts),
    book => (book.bookId, book.bookTitle, book.bookAuthors, book.bookLanguages,
      book.bookPublishers, book.bookPublishedDates, book.bookTags, book.bookSummary, book.bookParts))
}

trait BookComponent{
//  def couchclient:CouchbaseClient

  def saveBook(book:Book)(implicit couchclient:CouchbaseClient){
    val key = "book:"+book.bookId
    couchclient.set(key, 0, Json.toJson(book).toString)
    couchclient.set("cover:"+book.bookId, 0, book.bookCover)
  }

//  def saveBookPart(part:BookPart)(implicit couchclient:CouchbaseClient){
//    couchclient.set("part:"+part.partId, 0, part.content)
//  }

  def saveBookResources(bookId:UUID, zipedFile:ZipFile)(implicit couchclient:CouchbaseClient){
    EpubLoader.getResources(zipedFile).foreach{r =>
      couchclient.set(bookId+":"+r.getHref, 0, r.getData)
    }
  }

  /**
   * Returns a list of books sorted by popularity
   */
  def listBooks()(implicit couchclient:CouchbaseClient):List[Book] = {

    //Prepare the view and query
    val bookView = couchclient.getView("book","by_title")
    val query = new Query()

    // We don't want the full documents and only the top 20
    query.setIncludeDocs(true)//.setLimit(20)

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
      val cover = getBookCover(bookId)

      Book(bookId,row.getKey(),authors,Nil,Nil,publishedDates,tags,summary,cover,List[BookPart]())
    }.toList
  }

  def getBook(bookId:UUID)(implicit couchclient:CouchbaseClient):Option[Book] = {
    couchclient.get("book:"+bookId) match{
      case str:String => Json.parse(str).validate[Book].fold(
        err => {
          println(s"[ERROR] Invalid Json document with id ${bookId.toString}. Expected: Book")
          println("[ERROR] " + err)
          None
        },
        book => {
	        val cover = getBookCover(bookId)
	        Some(book.setCover(cover))
        }  
      )
      case _ => None
    }
  }

//  def getBookPart(bookId:UUID,partId:String)(implicit couchclient:CouchbaseClient):Option[BookPart] = {
//    val key = "part:"+partId
//    Some(BookPart(partId, "", getBookResource(bookId, partId)))
//  }

  def saveEzoomBook(ezb:Ezoombook)(implicit couchclient:CouchbaseClient){
    val key = "ezb:"+ezb.ezoombook_id
    couchclient.set(key, 0, Json.toJson(ezb).toString())
  }

  /**
   * Returns an eZoomBook
   * @param ezbId
   * @param couchclient
   * @return
   */
  def getEzoomBook(ezbId:UUID)(implicit couchclient:CouchbaseClient):Option[Ezoombook] = {
    couchclient.get("ezb:"+ezbId.toString) match {
      case str:String =>
        Json.parse(str).validate[Ezoombook].fold(
          err => {
            println(s"[ERROR] Could not parse document $ezbId as Ezoombook: $err")
            None
          },
          ezb => Some(ezb)
        )
      case _ =>
        println(s"[ERROR] Ezoombook $ezbId not found.")
        None
    }
  }

  /**
   * Returns the ezoobooks for a book
   */
  def getEzoomBooks(bookId:UUID)(implicit couchclient:CouchbaseClient):List[Ezoombook] = {
    //Prepare the view and query
    val view = couchclient.getView("ezb","by_bookid")
    val query = new Query()

    // We want the full documents for documents with key "bookId"
    query.setIncludeDocs(true).setKey(bookId.toString)

    // Send the query
    val result = couchclient.query(view,query)

    //Retrieve the array containing the ezoombooks
    result.foldLeft(List[Ezoombook]()){(lst,row) =>
      row.getDocument().map{doc =>
        val js = Json.parse(doc.asInstanceOf[String])
        js.validate[Ezoombook].fold(
          err => {
            println(s"[ERROR] Invalid Json document with book_id  = ${bookId.toString}. Expected: EzoomBook")
            println("[ERROR] " + err)
            lst
          },
          ezb => ezb +: lst
        )
      }.getOrElse(lst)
    }.toList
  }

  /**
   * Returns the eZoomBooks owned by a user
   */
  def getEzoombooksUser(uId:UUID)(implicit couchclient:CouchbaseClient):List[Ezoombook] = {
    val view = couchclient.getView("ezb", "by_owner")
    val query = (new Query()).setIncludeDocs(true).setKey(uId.toString)
    couchclient.query(view,query).foldLeft(List[Ezoombook]()){(lst,row) =>
      row.getDocument().map{doc =>
        val js = Json.parse(doc.asInstanceOf[String])
        js.validate[Ezoombook].fold(
          err => {
            println(s"[ERROR] Invalid Json document with ower_id = ${uId.toString}. Expected: EzoomBook")
            println("[ERROR] " + err)
            lst
          },
          ezb => ezb +: lst
        )
      }.getOrElse(lst)
    }.toList
  }

  def saveLayer(ezl:EzoomLayer)(implicit couchclient:CouchbaseClient):Option[Ezoombook] = {
    //Save the layer
    val key = "ezoomlayer:"+ezl.ezoomlayer_id
    couchclient.set(key, 0, Json.toJson(ezl).toString())

    //Get and update ezoombook
    val ezbKey = "ezb:"+ ezl.ezoombook_id
    couchclient.getAndLock(ezbKey, 15) match{
      case cas:CASValue[_] => Json.parse(cas.getValue().asInstanceOf[String]).validate[Ezoombook].fold(
        err => {
          println("[WARNING] Could not update ezoombok associated to ezoomlayer: " + err)
          None
        },
        ezb => {
          if(!ezb.ezoombook_layers.contains(ezl.ezoomlayer_id.toString)){
            val newEzb = Ezoombook(ezb.ezoombook_id,
              ezb.book_id,ezb.ezoombook_owner,ezb.ezoombook_status,
              ezb.ezoombook_title,ezb.ezoombook_public,ezb.ezoombook_layers + (ezl.ezoomlayer_level.toString -> ezl.ezoomlayer_id.toString))
            couchclient.cas(ezbKey, cas.getCas, Json.toJson(newEzb).toString())
          }
          Some(ezb)
        }
      )
      case _ =>
        println("[WARNING] Oops there is no ezoombook associated to this ezoomlayer")
        None
    }
  }

  def getLayer(ezlId:UUID)(implicit couchclient:CouchbaseClient):Option[EzoomLayer] = {
    val key = "ezoomlayer:"+ezlId //TODO Export this string
    couchclient.get(key) match{
      case str:String =>
        Json.parse(str).validate[EzoomLayer].fold(
          err => {
            println(s"[ERROR] Could not parse document $ezlId as EzoomLayer: $err")
            None
          },
          ezl => Some(ezl)
        )
      case _ =>
        println(s"[ERROR] Ezoomlayer $key not found.")
        None
    }
  }

  def getBookCover(bookId:UUID)(implicit couchclient:CouchbaseClient):Array[Byte] = {
    val key = "cover:"+bookId.toString
    couchclient.get(key) match{
      case arr:Array[Byte] => arr
      case _ => Array[Byte]()
    }
  }

  def getBookResource(bookId:UUID, resPath:String)(implicit couchclient:CouchbaseClient):Array[Byte] = {
    val key = bookId + ":" + resPath
    couchclient.get(key) match{
      case arr:Array[Byte] => arr
      case _ =>
        println(s"[ERROR] Resource with key $key not found")
        Array[Byte]()
    }
  }

//  def getBooksByAuthor(authName:String)(implicit couchclient:CouchbaseClient):List[Book] = {
//    val view = couchclient.getView("book","by_author")
//    val query = new Query()
//  }

  def deleteLayer(ezbId:UUID, layerLevel:Int)(implicit couchclient:CouchbaseClient):Boolean = {
    getEzoomBook(ezbId).flatMap{ezb =>
      ezb.ezoombook_layers.get(layerLevel.toString).map{layerId =>
        val modifiedEzb = Ezoombook.removeLayer(ezb, layerLevel.toString)
        saveEzoomBook(modifiedEzb)
        couchclient.delete(layerId).get().booleanValue()
      }
    }.getOrElse{
      println(s"[ERROR] Could not find eZoomBook with id $ezbId")
      false
    }
  }

  def deleteEzb(ezbId:UUID)(implicit couchclient:CouchbaseClient):Boolean = {
    //First delete layers
    getEzoomBook(ezbId).map{ezb =>
      //First delete layers
      ezb.ezoombook_layers.values.map(couchclient.delete(_).get().booleanValue()).
        fold(true)(_ && _) &&
      //Then delete ezb
      couchclient.delete("ezb:"+ezbId.toString).get().booleanValue()

    }.getOrElse{
      println(s"[ERROR] Could not find eZoomBook with id $ezbId")
      false
    }
  }
}
