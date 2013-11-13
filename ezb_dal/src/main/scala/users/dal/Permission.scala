package users.dal

import java.util.UUID

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 31/10/2013
 * Time: 17:09
 * To change this template use File | Settings | File Templates.
 */
sealed trait Permission extends Function1[User,Boolean]{
  def apply(user:User):Boolean
}

case object Administrator extends Permission{
  def apply(user:User):Boolean = {
    user.permission == this
  }
}
case object RegisteredUser extends Permission{
  def apply(user:User):Boolean = {
    user.permission == this || user.permission == Administrator
  }
}

case object Guest extends Permission{
  def apply(user:User):Boolean = {
    true
  }
}

object Permission{
  def valueOf(value:String):Permission = value match{
    case "Administrator" => Administrator
    case "RegisteredUser" => RegisteredUser
    case "Guest" => Guest
    case _ => throw new IllegalArgumentException()
  }
}
