package utils
 
object sha256 {
  class StringSHAHelper(str: String) {
    val md = java.security.MessageDigest.getInstance("SHA-256")
    def sha_256 = (new sun.misc.BASE64Encoder).encode(md.digest(str.getBytes))
  }
 
  implicit def stringWrapper(string: String) = new StringSHAHelper(string)
}
