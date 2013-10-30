package models

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 29/10/2013
 * Time: 11:59
 * To change this template use File | Settings | File Templates.
 */
sealed trait Permission

case object Administrator extends Permission
case object RegisteredUser extends Permission
case object Guest extends Permission

object Permission{
  def valueOf(value:String):Permission = value match{
    case "Administrator" => Administrator
    case "RegisteredUser" => RegisteredUser
    case _ => throw new IllegalArgumentException()
  }
}