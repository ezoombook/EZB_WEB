package users.dal

import java.util.UUID

/**
 * Created with IntelliJ IDEA.
 * User: gonto
 * Date: 11/23/12
 * Time: 9:47 PM
 * To change this template use File | Settings | File Templates.
 */

case class User(id: UUID, name: String, email: String, password: String)

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

    def add(user: User)(implicit session: Session) = {
      this.insert(user)
      println("[UDAL] User created: " + user.name)
    }

    def countByName(name: String)(implicit session: Session) = {
      (for {
        user <- Users
        if (user.name === name)
      } yield(user)).list.size
    }

    def all()(implicit session: Session) = {
      Query(Users).list
    }
  }
}

