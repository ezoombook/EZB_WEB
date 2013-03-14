package users.dal

import slick.driver.ExtendedProfile

/**
 * Created with IntelliJ IDEA.
 * User: gonto
 * Date: 11/23/12
 * Time: 11:47 PM
 * To change this template use File | Settings | File Templates.
 */

/**
 * The data access layer for the Users database.
 */ 
class UserDAL(override val profile: ExtendedProfile) extends UserComponent with GroupComponent with Profile {

  import profile.simple._

  /**
   * Helper method to create all the tables in the Users database
   * */
  def create(implicit session: Session): Unit = {
    Users.ddl.create 
    UserBooks.ddl.create
    Groups.ddl.create
    GroupMembers.ddl.create
  }
}

