package models

import users.dal.{User, Group}

import util.DynamicVariable
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

  def setUserMaxHistory(userId:UUID, maxItems:Int){
    AppDB.database.withSession{
      implicit session:Session =>
	AppDB.dal.UserPreferences.insert(userId, maxItems)
    }
  }

  def getUserMaxHistory(userId:UUID):Option[Int] = {
    AppDB.database.withSession{
      implicit session:Session =>
	AppDB.dal.UserPreferences.getMaxHistoryItems(userId)
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

  def userOwnedGroups(userId:UUID):List[Group] = {
    AppDB.database.withSession{
      implicit session:Session =>
	AppDB.dal.Groups.getUserGroups(userId)
    }
  }

  def userIsMemberGroups(userId:UUID):List[Group] = {
    AppDB.database.withSession{
      implicit session:Session =>
	AppDB.dal.GroupMembers.getGroupsByMember(userId).flatMap{gid =>
	  AppDB.dal.Groups.getGroup(gid).toList
	}
    }
  }

  def getGroupById(groupId:UUID) = {
    AppDB.database.withSession{
      implicit session:Session =>
      AppDB.dal.Groups.getGroup(groupId)
    }
  }

  def getGroupMembers(groupId:UUID):List[User] = {
    AppDB.database.withSession{
      implicit session:Session =>
      AppDB.dal.GroupMembers.getGroupMembers(groupId)
    }
  }

  def newGroupMember(groupId:UUID, membId:UUID, memRole:String) = {
    AppDB.database.withSession{
      implicit session:Session =>      
      AppDB.dal.GroupMembers.addMember(groupId, membId, memRole)
    }
  }

  def getGroupMemberRole:Seq[(String,String)] = {
    AppDB.dal.Roles.values.map{v => (v.toString, v.toString.capitalize)}.toSeq
  }
}
