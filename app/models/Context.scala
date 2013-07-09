package models

import users.dal.User
import books.dal.{Ezoombook,EzoomLayer}

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 04/06/13
 * Time: 15:22
 * To change this template use File | Settings | File Templates.
 */
case class Context (user: Option[User], activeEzb:Option[Ezoombook], activeLayer:Option[EzoomLayer])
