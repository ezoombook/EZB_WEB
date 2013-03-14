package models

import users.dal.User

import java.util.UUID

object UserDO{

  import AppDB.dal.profile.simple._

  def listUsers:List[User] = AppDB.database.withSession{
      implicit session:Session =>
	AppDB.dal.Users.all()
  }

  def create(user:User) {
    AppDB.database.withSession{
      implicit session:Session =>
	AppDB.dal.Users.add(user)
    }
  }

  def validateUser(user:String, password:String):Boolean = {
    AppDB.database.withSession{
      implicit session:Session =>
	AppDB.dal.Users.validateUserPassword(user, password)
    }
  }

  def getUser(username:String):Option[UUID] = {
    AppDB.database.withSession{
      implicit session:Session =>
	AppDB.dal.Users.getUserId(username)
    }
  }

  def newUserBook(userId:UUID, bookId:UUID) = {
    AppDB.database.withSession{
      implicit session:Session =>
	AppDB.dal.UserBooks.add(userId, bookId)
    }
  }

  def listBooks(userId:UUID):List[(String, Long)] = {
    AppDB.database.withSession{
      implicit session:Session =>
	AppDB.dal.UserBooks.getBooksByUser(userId).map(b => ("book:", b._2))
    }
  }

  def newGroup(groupName:String, ownerId:UUID) = {
    AppDB.database.withSession{
      implicit session:Session =>
	AppDB.dal.Groups.add(groupName, ownerId)
    }
  }
 
}
