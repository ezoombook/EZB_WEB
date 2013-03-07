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

    def id = column[UUID]("id", O.PrimaryKey)
    def name =  column[String]("name", O.NotNull)
    def email = column[String]("email", O.NotNull)
    def password = column[String]("password", O.NotNull)
    def * = id ~ name ~ email ~ password <> (User, User.unapply _)

/*
    val userByNameOrMail = for {
      id <- Parameters[String]
      u <- Users if (u.name.equals(id)) //|| u.email.equals(id))
    } yield u
*/

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
//      val user = userByNameOrMail(username).first
      Query(Users).filter(_.name === username).map(_.password).firstOption map(
	  Hasher.compare(pass, _)
      ) getOrElse(false)      
    }

  }

}

