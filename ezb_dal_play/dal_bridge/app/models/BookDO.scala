package models

import books.dal._
import books.util._

import java.io.{InputStream, FileInputStream, ByteArrayInputStream, File}

object BooksDO{

  def newBook(byteArray: Array[Byte]):Book = newBook(new ByteArrayInputStream(byteArray))

  def newBook(file: File):Book = newBook(new FileInputStream(file))  

  def newBook(in: InputStream):Book = EpubLoader.loadBook(in)
}
