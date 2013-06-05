package utils

import views.html.helper.FieldConstructor
import views.html._

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 30/04/13
 * Time: 11:14
 * To change this template use File | Settings | File Templates.
 */
object EzbHelpers {
  implicit val myfields = FieldConstructor(ezlayerFieldTemplate.f)
}

object navHelpers {
  implicit val myfields = FieldConstructor(navTemplate.f)
}
