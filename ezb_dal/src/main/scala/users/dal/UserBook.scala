package users.dal

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 07/03/13
 * Time: 17:37
 * To change this template use File | Settings | File Templates.
 */
case class UserBook(userId:UUID, bookId:UUID, book_created:Date)

trait UserBookComponent{
  this: Profile =>

  import profile.simple._

  object UserBook
}