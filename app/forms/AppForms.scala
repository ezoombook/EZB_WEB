package forms

import play.api.data.Form
import play.api.data.Forms._

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 04/06/13
 * Time: 17:09
 * To change this template use File | Settings | File Templates.
 */
object AppForms {

  val loginForm = Form(
    tuple(
      "id" -> text,
      "password" -> text
    ))

}
