package example

import books.dal._

import nl.siegmann.epublib.epub._

import scala.collection.JavaConversions._
import play.api.libs.json.Json

import java.io.{FileInputStream,ByteArrayInputStream,StringWriter}
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipFile
import books.util.EpubLoader
import org.apache.commons.io.IOUtils

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 03/04/13
 * Time: 16:32
 * To change this template use File | Settings | File Templates.
 */
object EpubExample extends App{
  val epubReader = new EpubReader()
  val bookFile = "/Users/mayleen/Downloads/leEbook.epub"
  val epub = epubReader.readEpub(new ZipFile(bookFile))
  val meta = epub.getMetadata()

  val bookId = UUID.randomUUID()

  val parts:List[BookPart] = (for(r <- epub.getContents) yield {
    val partId = bookId +":"+ UUID.randomUUID
    //    bdal.addPart(partId, r.getData)
    new BookPart(partId, bookId, r.getData)
  }).toList

  implicit class optionOps[+A](list: java.util.List[A]){
    def getOrElse[B >: A](index:Int, default: => B):B = {
      if (index < 0 || index >= list.size())
        default
      else
        list.get(index)
    }
  }
}

object PlayWithEpubFile extends App{
  val bookFile = "/Users/mayleen/Downloads/leEbook.epub"
//  val bookFile = "/Users/mayleen/Downloads/The Complete Works of HP Lovecraft.epub"
  val fin = new FileInputStream(bookFile)
  val zin = new ZipInputStream(fin)

  val zipfile = new ZipFile("/Users/mayleen/Downloads/leEbook.epub")
  val entries = zipfile.entries()
  while(entries.hasMoreElements){
    val entry = entries.nextElement()
    print("zip-elem: " + entry)
  }
  zipfile.close()
}

object ExampleWithEpubLoader extends App{
  val book = EpubLoader.loadBook(Right(new ZipFile("/Users/mayleen/Downloads/leEbook.epub")))
  println("Ze book: " + book)
}

import books.util.Transformer

object EZBLoader extends App{
  val path = "/Users/mayleen/Documents/eZoomBook/colab_sample/RELNA DOU FEVRE.txt"

  val lines = scala.io.Source.fromFile(path).getLines.toSeq

  val result = Transformer(lines)
  if(result.isRight){
    val jsonResult = result.right.get
    println(Json.prettyPrint(jsonResult))
    jsonResult.validate[EzoomLayer].fold(
      valid = (res => println("A valid ezoomlayer " + res)),
      invalid = (e => println("Invalid object: " + e))
    )
  }else
    println("Error: " + result.left.get)
}