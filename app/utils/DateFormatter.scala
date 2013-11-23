package utils

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 13/06/13
 * Time: 18:55
 * To change this template use File | Settings | File Templates.
 */
object DateFormatter {
  def dateFormat(date:Long, pattern:String):String = {
    val sdf = new SimpleDateFormat(pattern)
    sdf.setTimeZone(TimeZone.getDefault)
    sdf.setLenient(false)

    sdf.format(new Date(date))
  }
}
