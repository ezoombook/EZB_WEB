package users.dal

import users.util.Hasher
import java.util.UUID
import java.util.Date

/**
 * Created with IntelliJ IDEA.
 * User: MRL
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
      projection.insert(user.id, user.name, user.email, user.hashedPassword)
    }

    def all()(implicit session: Session) = {
      Query(Users).list
    }

    def changePassword(uid:UUID, newPwd:String)(implicit session:Session) = {
      val uquery = Users.filter(u => u.id === uid).map(_.password)
      uquery.update(Hasher.hash(newPwd) )
    }

    def delete(uid:UUID)(implicit session:Session) = {
      Query(Users).filter(_.id === uid).delete
    }

    def validateUserPassword(username:String, pass:String)(implicit session:Session): Boolean = {
      Query(Users).filter(u => u.name === username || u.email === username).map(_.password).firstOption map(
	  Hasher.compare(pass, _)
      ) getOrElse(false)      
    }

    def getUserId(username:String)(implicit session:Session) = {
      Query(Users).filter(u => u.name === username || u.email === username).map(_.id).firstOption
    }

  }

  object UserBooks extends Table[(UUID, UUID, Long)]("user_books"){
    def userId = column[UUID]("user_id", O.NotNull)
    def bookId = column[UUID]("book_id", O.NotNull)
    def dateCreated = column[Long]("book_date_created", O.NotNull) //Stored as Long to keep compatibility between DBs
    def * = userId ~ bookId ~ dateCreated

    def user = foreignKey("user_id", userId, Users)(_.id)

    def add(user_id:UUID, book_id:UUID)(implicit session:Session) = {
      val proj = UserBooks.userId ~ UserBooks.bookId ~ UserBooks.dateCreated
      proj.insert(user_id, book_id, new Date().getTime())
    }

    def getBooksByUser(user_id:UUID)(implicit session:Session) = {
      Query(UserBooks).filter(_.userId === user_id.bind).map(b => (b.bookId,b.dateCreated)).list
    }

    def delete(user_Id:UUID, book_Id:UUID)(implicit session:Session) = {
      Query(UserBooks).filter(ub => ub.userId === user_Id && ub.bookId === book_Id).delete
    }
  }
}

