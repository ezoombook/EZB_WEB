package controllers

import models._
import users.dal._

import scala.concurrent.{ExecutionContext, Future}

import play.api.i18n.Messages
import play.api.mvc._
import play.api.mvc.Results._
import play.api.Play._
import play.api.Logger
import play.api.cache.Cache
import reflect.classTag
import jp.t2v.lab.play2.auth._
import jp.t2v.lab.play2.stackc.{RequestWithAttributes, RequestAttributeKey, StackableController}

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 29/10/2013
 * Time: 11:43
 * To change this template use File | Settings | File Templates.
 */
trait AuthConfigImpl extends AuthConfig {

  /**
   * Type that idenfies a user
   */
  type Id = java.util.UUID

  /**
   * Type that represents a user
   */
  type User = users.dal.User

  /**
   * Type that is defined for authorization
   */
  type Authority = User => Boolean

  /**
   * A `ClassManifest` is used to retrieve an id from the Cache API.
   */
  val idTag = classTag[Id]

  /**
   * The session timeout in seconds
   */
  val sessionTimeoutInSeconds: Int = 3600

  /**
   * A function that returns a `User` object from an `Id`.
   * You can alter the procedure to suit your application.
   */
  def resolveUser(id: Id)(implicit ctx:ExecutionContext): Future[Option[User]] = {
    Future.successful(UserDO.getUser(id))
  }

  /**
   * Where to redirect the user after a successful login.
   */
  def loginSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[SimpleResult] =
    Future.successful(Redirect(routes.Workspace.home))

  /**
   * Where to redirect the user after logging out
   */
  def logoutSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[SimpleResult] =
    Future.successful(Redirect(routes.Application.login).withNewSession)

  /**
   * If the user is not logged in and tries to access a protected resource then redirct them as follows:
   */
  def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[SimpleResult] =
    Future.successful(Redirect(routes.Application.login).withNewSession)

  /**
   * If authorization failed (usually incorrect password) redirect the user as follows:
   */
  def authorizationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[SimpleResult] =
    Future.successful(Unauthorized(views.html.error(Messages("application.errorpage.unauthorized"))))

  /**
   * A function that determines what `Authority` a user has.
   */
  def authorize(user: User, authority: Authority)(implicit ctx: ExecutionContext): Future[Boolean] = Future.successful {
    authority(user)
  }

  /**
   * Whether use the secure option or not use it in the cookie.
   * However default is false, I strongly recommend using true in a production.
   */
  override lazy val cookieSecureOption: Boolean = false//play.api.Play.isProd(play.api.Play.current)

}
