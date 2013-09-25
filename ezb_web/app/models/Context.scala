package models

import users.dal._
import books.dal.{Ezoombook,EzoomLayer}

import play.api.i18n.Lang

import java.util.UUID

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 04/06/13
 * Time: 15:22
 * To change this template use File | Settings | File Templates.
 */
case class Context (user: Option[User],
                    preferences: Option[Preferences],
                    supportedLanguages:Seq[Lang],
                    activeEzb:Option[UUID],
                    activeLayer:Option[UUID])


