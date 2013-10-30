package forms

import models._
import ezb.comments.Comment
import utils.FormHelpers

import play.api.data.Form
import play.api.data.Forms._
import java.util.{UUID,Date}
import play.api.i18n.Messages

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 04/06/13
 * Time: 17:09
 * To change this template use File | Settings | File Templates.
 */
object AppForms extends FormHelpers{

  val loginForm = Form{
    mapping(
      "id" -> text,
      "password" -> text
    )(UserDO.authenticate)(_.map(u => (u.email, "")))
      .verifying(Messages("application.login.failed"), result => result.isDefined)
  }

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

  val contactForm = Form(
    tuple(
      "usermail" -> text,
      "arequest" -> text
    )
  )

  val passwordForm = Form(
    tuple(
      "password1" -> text,
      "password2" -> text
    )
  )

  val passwordrForm = Form(
    tuple(
      "email" -> text,
      "password" -> text
    )
  )

  val localeForm = Form("locale" -> nonEmptyText)

}
