package models

import users.dal.{User, Group, Preferences}

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

  def authenticate(id:String, password:String):Option[User] = {
    AppDB.database.withSession{
      implicit session:Session =>
        AppDB.dal.Users.authenticate(id, password)
    }
  }

  @deprecated("Use authenticate instead", since="1.0")
  def validateUser(user:String, password:String):Boolean = {
    AppDB.database.withSession{
      implicit session:Session =>
	    AppDB.dal.Users.validateUserPassword(user, password)
    }
  }

  def changePassword(uid:UUID, newPwd:String){
    AppDB.database.withSession{implicit session:Session =>
      AppDB.dal.Users.changePassword(uid, newPwd)
    }
  }

  def getUserId(username:String):Option[UUID] = {
    AppDB.database.withSession{
      implicit session:Session =>
        AppDB.dal.Users.getUserId(username)
    }
  }

  def getUser(username:String):Option[User] = {
    AppDB.database.withSession{implicit session:Session =>
	    AppDB.dal.Users.getUser(username)
    }
  }

  def getUser(uid:UUID):Option[User] = {
    AppDB.database.withSession{implicit session:Session =>
      AppDB.dal.Users.getUser(uid)
    }
  }

  def setUserMaxHistory(userId:UUID, maxItems:Int){
    AppDB.database.withSession{
      implicit session:Session =>
	      AppDB.dal.UserPreferences.setMaxHistory(userId, maxItems)
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

  def getGroupMembers(groupId:UUID):List[(User, AppDB.dal.Roles.Value)] = {
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

  def getUserPreferences(uid:UUID):Option[Preferences] = {
    AppDB.database.withSession{
      implicit session:Session =>
        AppDB.dal.UserPreferences.getUserPreferences(uid)
    }
  }

  def getUserLanguage(uid:UUID):String = {
    AppDB.database.withSession{
      implicit session:Session =>
        AppDB.dal.UserPreferences.getLanguage(uid).getOrElse("en")
    }
  }

  def setUserLanguage(uid:UUID, lang:String){
    AppDB.database.withSession{
      implicit session:Session =>
        AppDB.dal.UserPreferences.setLanguage(uid, lang)
    }
  }
}
