package util

import collection.{JavaConversions,mutable}
import JavaConversions._
import mutable.ArrayBuffer
import java.net.URI
import scala.util.{Try, Success, Failure}
import play.api.libs.json._
import com.couchbase.client.CouchbaseClient
import com.couchbase.client.protocol.views.DesignDocument
import com.couchbase.client.protocol.views.ViewDesign
import java.io.{StringWriter, FileInputStream, InputStreamReader}

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 17/07/13
 * Time: 15:27
 * To change this template use File | Settings | File Templates.
 */
object ViewInstaller extends App{
  val uris = ArrayBuffer(URI.create("http://localhost:8091/pools"))
  val bucket = "ezoom-books"
  val password = "ezoomwiki"

  val client = new CouchbaseClient(uris, bucket, password)

  //The list of design documents
  val docs = List("dev_book","dev_ezb","dev_projects","dev_votes") //...

  for(docName <- docs){
    saveDoc(docName) match{
      case Success(v) if v => println(s"Desing document $docName successfully stored!")
      case Success(v) if !v => println(s"Desing document $docName could not be stored. Please check your couchbase connection.")
      case Failure(e) => println(s"[ERROR] Could not create design document $docName : ${e.getMessage}" )
    }
  }

  client.shutdown()

  def saveDoc(docName:String):Try[Boolean] = {
    //Get view-docs from json file
    val path = s"resources/$docName.ddoc"
    val source = Try(scala.io.Source.fromFile(path))

    source.flatMap{ s =>
      val lines = s.getLines.mkString
      s.close()

      val designDoc = new DesignDocument(docName)

      //Parse the document to obtain views' definitions
      Json.parse(lines)\"views" match{
        case jo:JsObject =>
          jo.value.foreach{ v =>
            val viewName = v._1
            val mapFun = v._2\"map"
            val redFun = v._2\"reduce"

            ((mapFun.asOpt[String], redFun.asOpt[String]) match{
              case (Some(map), Some(reduce)) => Success(new ViewDesign(viewName, map, reduce))
              case (Some(map), None) => Success(new ViewDesign(viewName, map))
              case _ => //Error the view needs to have at least a map function
                throw new RuntimeException(s"Error creating $viewName view, map function not defined!")
            }).map{viewDesign =>
              designDoc.getViews().add(viewDesign)
            }
          }
          Success(client.createDesignDoc( designDoc ))
        case _ => //println(s"Not a valid Json object in design document $docName")
          Failure(new RuntimeException(s"Not a valid Json object in design document $docName"))
      }
    }
  }

}

