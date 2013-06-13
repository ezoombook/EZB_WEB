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

/**
 * The User entitiy
 */
case class User(id: UUID, name: String, email: String, password: String){
  val hashedPassword = Hasher.hash(password)
}

/**
 * A component to manage the Users and UserBooks tables. 
 */ 
trait UserComponent {
  this: Profile =>

  import profile.simple._

  /**
   * The Users table object
   */ 
  object Users extends Table[User]("users") {
    def id = column[UUID]("user_id", O.PrimaryKey, O.DBType("UUID"))
    def name =  column[String]("user_name", O.NotNull)
    def email = column[String]("user_email", O.NotNull)
    def password = column[String]("user_password", O.NotNull)
    def * = id ~ name ~ email ~ password <> (User, User.unapply _)

    /**
     * Adds a new user to the table
     */ 
    def add(user: User)(implicit session: Session) = {
      val projection = Users.id ~ Users.name ~ Users.email ~ Users.password
      projection.insert(user.id, user.name, user.email, user.hashedPassword)
    }

    /**
     * List all the users in the Users table
     */ 
    def all()(implicit session: Session) = {
      Query(Users).list
    }

    /**
     * Sets a new password for a  user
     * @param uid Identifier of the user
     */ 
    def changePassword(uid:UUID, newPwd:String)(implicit session:Session) = {
      val uquery = Users.filter(u => u.id === uid).map(_.password)
      uquery.update(Hasher.hash(newPwd) )
    }

    /**
     * Removes a user from the Users table
     **/
    def delete(uid:UUID)(implicit session:Session) = {
      Query(Users).filter(_.id === uid).delete
    }

    /**
     * Checks if the password is valid for a user by its user-name or mail 
     */ 
    def validateUserPassword(username:String, pass:String)(implicit session:Session): Boolean = {
      Query(Users).filter(u => u.name === username || u.email === username).map(_.password).firstOption map(
	  Hasher.compare(pass, _)
      ) getOrElse(false)      
    }

    /**
     * Returns the user-ID of a user by user-name or mail
     */ 
    def getUserId(username:String)(implicit session:Session) = {
      Query(Users).filter(u => u.name === username || u.email === username).map(_.id).firstOption
    }

    /**
     * Returns the complete User object, searching by user-name or mail
     */
    def getUser(username:String)(implicit session:Session) = {
      Query(Users).filter(u => u.name === username || u.email === username).firstOption
    }

    /**
     * Returns a User, searching by user-id
     */
    def getUser(uid:UUID)(implicit session:Session) = {
      Query(Users).filter(u => u.id == uid.bind).firstOption
    }
  }

  /**
   * The UserBooks table object
   */ 
  object UserBooks extends Table[(UUID, UUID, Long)]("user_books"){
    def userId = column[UUID]("user_id", O.NotNull, O.DBType("UUID"))
    def bookId = column[UUID]("book_id", O.NotNull, O.DBType("UUID"))
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

  /**
   * The users_preferences table object.
   * Unitially we only have the number of items in the history,
   * but new preference items can be added later
   */
  object UserPreferences extends Table[(UUID, Int)]("users_preferences"){
    def userId = column[UUID]("user_id", O.PrimaryKey, O.DBType("UUID"))
    def maxHistory = column[Int]("preferences_max_history")
    def * = userId ~ maxHistory

    def user = foreignKey("fk_user", userId, Users)(_.id)

    def getMaxHistoryItems(user_id:UUID)(implicit session:Session):Option[Int] = {
      Query(UserPreferences).filter(_.userId === user_id.bind).map(_.maxHistory).firstOption 
    }
  }
}

