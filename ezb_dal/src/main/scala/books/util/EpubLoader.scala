package books.util

import books.dal.{Book, BookPart}

import java.util.UUID
import java.io.InputStream
import nl.siegmann.epublib.epub._
import scala.collection.JavaConversions._

object EpubLoader{

  def loadBook(in: InputStream):Book = {
    val epubReader = new EpubReader()
    val epub = epubReader.readEpub(in)
    val meta = epub.getMetadata()

    val bookId = UUID.randomUUID()

    val parts:List[BookPart] = (for(r <- epub.getContents) yield {
      new BookPart(bookId +":"+ UUID.randomUUID, bookId, r.getData)
    }).toList

    new Book(bookId, 
	      /* Title  */ meta.getFirstTitle(),
	      /* Authrs */ meta.getAuthors().map(a => a.getFirstname() + " " + a.getLastname()).toList, 
	      /* Langs  */ List(meta.getLanguage),
	      /* Publsr */ meta.getPublishers().toList,
	      /* Date   */ meta.getDates().map(_.toString).toList,
	      /* Tags   */ meta.getSubjects().toList,
	      /* Summry */ meta.getDescriptions.getOrElse(0, ""),
	      /* Parts  */ parts)    
  }      

  implicit class optionOps[+A](list: java.util.List[A]){
    def getOrElse[B >: A](index:Int, default: => B):B = {
      if (index < 0 || index >= list.size())    
        default
      else
        list.get(index)
    } 
  }

}


