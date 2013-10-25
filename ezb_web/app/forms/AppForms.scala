package forms

import ezb.comments.Comment
import utils.FormHelpers

import play.api.data.Form
import play.api.data.Forms._
import java.util.{UUID,Date}

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 04/06/13
 * Time: 17:09
 * To change this template use File | Settings | File Templates.
 */
object AppForms extends FormHelpers{

  val loginForm = Form(
    tuple(
      "id" -> text,
      "password" -> text
    ))

  def commentForm = Form[Comment](
    mapping(
      "commentId" -> default(of[UUID], UUID.randomUUID),
      "commentAuthor" -> of[UUID],
      "commentEzb" -> of[UUID],
      "commentLayer" -> of[UUID],
      "commentPart" -> optional(text),
      "commentContrib" -> of[UUID],
      "contribAuthor" -> of[UUID],
      "contribType" -> text,
      "commentDate" -> default(date, new Date()),
      "commentContent" -> text
    )(Comment.apply)(Comment.unapply _)
  )
}
