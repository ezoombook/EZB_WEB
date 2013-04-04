package models

import books.dal._
import books.util._

import java.io.{InputStream, FileInputStream, ByteArrayInputStream, File}
import java.util.zip.ZipFile

object BooksDO{

  def newBook(byteArray: Array[Byte]):Book = EpubLoader.loadBook(Left(new ByteArrayInputStream(byteArray)))

  def newBook(file: File):Book = EpubLoader.loadBook(Right(new ZipFile(file)))

}
