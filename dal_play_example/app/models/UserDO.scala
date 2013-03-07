package models

import users.dal.User

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
}
