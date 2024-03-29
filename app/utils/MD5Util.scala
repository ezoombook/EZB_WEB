package utils

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 05/06/13
 * Time: 18:39
 * To change this template use File | Settings | File Templates.
 */
object MD5Util {
  val md = java.security.MessageDigest.getInstance("MD5")

  def hex(bytes:Array[Byte]):String = {
    val sb = new StringBuffer()

    for(b <- bytes){
      sb.append(Integer.toHexString(b & 0xFF | 0x100).substring(1,3))
    }
    sb.toString
  }

  def md5Hex(str:String):String = {
    hex(md.digest(str.getBytes("CP1252")))
  }

}
