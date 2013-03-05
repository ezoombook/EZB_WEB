package users.util

import org.mindrot.jbcrypt.BCrypt

/**
Utility class to hash passwords
*/
object Hasher{
  def hash(password:String):String = BCrypt.hashpw(password, BCrypt.gensalt)
  def compare(password:String, hashedPassword:String):Boolean = BCrypt.checkpw(password, hashedPassword)
}

