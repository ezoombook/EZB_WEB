package users.dal

import users.util.Hasher
import java.util.UUID

/**
 * Created with IntelliJ IDEA.
 * User: gonto
 * Date: 11/23/12
 * Time: 9:47 PM
 * To change this template use File | Settings | File Templates.
 */

case class User(id: UUID, name: String, email: String, password: String){
  val hashedPassword = Hasher.hash(password)
}

trait UserComponent {
  this: Profile =>

  import profile.simple._

  object Users extends Table[User]("users") {
    var uid = 1

    def id = column[UUID]("user_id", O.PrimaryKey)
    def name =  column[String]("user_name", O.NotNull)
    def email = column[String]("user_email", O.NotNull)
    def password = column[String]("user_password", O.NotNull)
    def * = id ~ name ~ email ~ password <> (User, User.unapply _)

    def add(user: User)(implicit session: Session) = {
      val projection = Users.id ~ Users.name ~ Users.email ~ Users.password
      val created = projection.insert(user.id, user.name, user.email, user.hashedPassword)
      println("[UDAL] User created: " + user.name)
      created
    }

    def all()(implicit session: Session) = {
      Query(Users).list
    }

    def changePassword(uid:UUID, newPwd:String)(implicit session:Session) = {
      val uquery = Users.filter(u => uid == u.id).map(_.password)
      uquery.update(Hasher.hash(newPwd) )
    }

    def delete(uid:UUID)(implicit session:Session) = {
      Query(Users).filter(_.id.equals(uid)).delete
    }

    def validateUserPassword(username:String, pass:String)(implicit session:Session): Boolean = {
      Query(Users).filter(_.name === username).map(_.password).firstOption map(
	  Hasher.compare(pass, _)
      ) getOrElse(false)      
    }

  }

  object UserBook extends Table[UUID, UUID, Date]("user_books"){
    def userId = column[UUID]("user_id")
    def bookId = column[UUID]("book_id")
    def date_created = column[Date]("book_date_created")

    def user = foreignKey("user_id", userId, Users)(_.id)
  }
}

