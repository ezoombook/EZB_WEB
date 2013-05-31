import books.dal._
import books.util.Transformer
import books.util.UUIDjsParser
import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._

object EZBLoader extends App with UUIDjsParser{
  import play.api.libs.functional.syntax._

  val path = "/Users/mayleen/Documents/eZoomBook/colab_sample/RELNA DOU FEVRE.txt"

  val lines = scala.io.Source.fromFile(path).getLines.toSeq

  val result = Transformer(lines)
  if(result.isRight){
    val jsonResult = result.right.get
        //println(Json.prettyPrint(jsonResult))
    jsonResult.validate[EzoomLayer](AltFormats.ezlReads(UUID.randomUUID)).fold(
      valid = {res =>
        val jsRes = Json.toJson(res)//(AltFormats.ezlWrites)
        println("The ezb in Json: " + jsRes)
      },
      invalid = (e => println("Invalid object: " + e))
    )
  }else
    println("Error: " + result.left.get)

  def defaultValue[T](v:T)(implicit r:Reads[Option[T]]):Reads[T] = r.map(_.getOrElse(v))

  def debugger[T](implicit r:Reads[T]):Reads[T] = {
    println("[DEBUG] value = " + r.map(_.toString))
    r
  }
}
