package books.util

import books.dal.{Book, BookPart}

import java.util.UUID
import java.io.InputStream
import nl.siegmann.epublib.epub.EpubReader
import nl.siegmann.epublib.domain._
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

    val parts = partList(epub.getTableOfContents.getTocReferences.toList)

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

  def partList(refList:List[TOCReference]):List[BookPart] = {

    def ref2Part(r:TOCReference):BookPart = {
      new BookPart(r.getCompleteHref, toOpt(r.getTitle))
    }

    def partListRec(acum:List[BookPart], rlst:java.util.List[TOCReference]):List[BookPart] = {
      val scalaList:List[TOCReference] = rlst.toList
      scalaList match{
        case Nil => acum
        case r::rest => partListRec((acum :+ ref2Part(r)) ++ partListRec(Nil, r.getChildren), rest)
      }
    }

    partListRec(Nil, refList)
  }

  private def toOpt[T](x:T):Option[T] = if(x == null) None else Some(x)
}

object Testin extends App{
  val epubReader = new EpubReader()
  val bookFile = "/Users/mayleen/Downloads/Le comte de monte christo.epub"
  val epub = epubReader.readEpub(new ZipFile(bookFile))
  val meta = epub.getMetadata()

  val bookId = UUID.randomUUID()

  //Toc in List[TOCReference] form
  val toc = EpubLoader.partList(epub.getTableOfContents.getTocReferences.toList)

  println("Toc:")
  for (part <- toc){
    println("-" + part.title + " -> " + part.partId)
  }


}
