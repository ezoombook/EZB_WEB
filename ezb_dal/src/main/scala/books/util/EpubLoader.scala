package books.util

import books.dal.{Book, BookPart}

import java.util.UUID
import java.io.InputStream
import nl.siegmann.epublib.epub.EpubReader
import nl.siegmann.epublib.domain.Resource
import scala.collection.JavaConversions._
import java.util.zip.ZipFile

object EpubLoader{

  def loadBook(in: Either[InputStream,ZipFile]):Book = {
    val epub = readEpub(in)
    val meta = epub.getMetadata()

    val bookId = UUID.randomUUID()

    val toc = (for(r <- epub.getTableOfContents.getTocReferences) yield{
      r.getCompleteHref -> r.getTitle
    }).toMap

    val parts = (for(r <- epub.getContents) yield {
      new BookPart(r.getHref, toc.get(r.getHref))
    }).toList

    new Book(bookId, 
	      /* Title  */ meta.getFirstTitle(),
	      /* Authrs */ meta.getAuthors().map(a => a.getFirstname() + " " + a.getLastname()).toList, 
	      /* Langs  */ List(meta.getLanguage),
	      /* Publsr */ meta.getPublishers().toList,
	      /* Date   */ meta.getDates().map(_.toString).toList,
	      /* Tags   */ meta.getSubjects().toList,
	      /* Summry */ meta.getDescriptions.getOrElse(0, ""),
	      /* Cover  */ toOpt(epub.getCoverImage()).map(_.getData()).getOrElse(Array[Byte]()),
	      /* Parts  */ parts)    
  }      

  def readEpub(in: Either[InputStream,ZipFile]) = {
    val epubReader = new EpubReader()
    in.fold(epubReader.readEpub(_), epubReader.readEpub(_))
  }

  def getResources(in: ZipFile):List[Resource] = {
    readEpub(Right(in)).getResources.getAll.toList
  }

  implicit class optionOps[+A](list: java.util.List[A]){
    def getOrElse[B >: A](index:Int, default: => B):B = {
      if (index < 0 || index >= list.size())    
        default
      else
        list.get(index)
    } 
  }

  private def toOpt(res:Resource):Option[Resource] = {
    if (res == null)
      None
    else
      Some(res)
  }
}

object Testin extends App{
  val epubReader = new EpubReader()
  val bookFile = "/Users/mayleen/Downloads/Le comte de monte christo.epub"
  val epub = epubReader.readEpub(new ZipFile(bookFile))
  val meta = epub.getMetadata()

  val bookId = UUID.randomUUID()

  val toc = (for(r <- epub.getTableOfContents.getTocReferences) yield{
    r.getCompleteHref -> r.getTitle
  }).toMap
  println("Toc:")
  println(toc.mkString("\n"))

  println("epub.getContents = ")
  for(c <- epub.getContents){
    val title = toc.getOrElse(c.getHref, c.getHref)

    println("- " + title)
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
