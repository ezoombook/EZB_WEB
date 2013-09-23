package models

import users.dal._
import books.dal.{Ezoombook,EzoomLayer}

import play.api.i18n.Lang

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
                    activeEzb:Option[Ezoombook], /* TODO change to ref instead (remove from cache) */
                    activeLayer:Option[EzoomLayer]) /* TODO change to ref idem */


