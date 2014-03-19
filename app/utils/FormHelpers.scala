package utils

import books.dal.Status
import play.api.data.format.Formatter
import play.api.data.{FormError, Mapping}
import play.api.data.Forms._
import play.api.data.format.Formats._

import scala.util.{Try, Success, Failure}

import java.util.UUID

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 12/06/13
 * Time: 20:14
 * To change this template use File | Settings | File Templates.
 */
trait FormHelpers {
  implicit def str2Status(strVal: String): Status.Value = Status.withName(strVal)

  implicit def statusFormat: Formatter[Status.Value] = new Formatter[Status.Value] {
    def bind(key: String, data: Map[String, String]) =
      data.get(key).map(Status.withName(_)).toRight(Seq(FormError(key, "error.required", Nil)))

    def unbind(key: String, value: Status.Value): Map[String, String] = Map(key -> value.toString)
  }

  implicit def uuidForam: Formatter[UUID] = new Formatter[UUID] {
    def bind(key: String, data: Map[String, String]) =
      data.get(key).map {
        id =>
          Try(UUID.fromString(id)) match {
            case Success(uid) => Right(uid)
            case Failure(err) => Left(Seq(FormError(key, "error.invalid.uuid", Nil)))
          }
      }.getOrElse(Left(Seq(FormError(key, "error.required", Nil))))

    def unbind(key: String, value: UUID): Map[String, String] = Map(key -> value.toString)
  }

  import java.util.{Date, TimeZone}
  import java.text.SimpleDateFormat

  def dateAsLong(pattern: String, timeZone: TimeZone = TimeZone.getDefault): Mapping[Long] =
    of[Long] as dateLongFormat(pattern, timeZone)

  def dateLongFormat(pattern: String, timeZone: TimeZone = TimeZone.getDefault): Formatter[Long] = new Formatter[Long] {

    val sdf = new SimpleDateFormat(pattern)
    sdf.setTimeZone(timeZone)
    sdf.setLenient(false)

    def bind(key: String, data: Map[String, String]) =
      dateFormat(pattern).bind(key, data).fold(
        err => Left(err),
        date => Right(date.getTime)
      )

    def unbind(key: String, value: Long): Map[String, String] = Map(key -> sdf.format(new Date(value)))
  }

//  implicit def str2uuidable(str: String): UUIDableString = new UUIDableString(str)
//
//  class UUIDableString(str: String) {
//    def toUUID: Either[String, UUID] = {
//      Try(UUID.fromString(str)) match {
//        case Success(uid) => Right(uid)
//        case Failure(err) => Left("Invalid UUID " + str)
//      }
//    }
//  }

}
