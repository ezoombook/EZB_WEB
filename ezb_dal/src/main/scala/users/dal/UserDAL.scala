package users.dal

import slick.driver.ExtendedProfile
import slick.jdbc.meta.MTable
import slick.lifted.DDL

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
    implicit val tableMap:Map[String, MTable] = makeTableMap

    Users.createIfNotDefined (tableMap)
    UserBooks.createIfNotDefined (tableMap)
    UserPreferences.createIfNotDefined (tableMap)
    Groups.createIfNotDefined (tableMap)
    GroupMembers.createIfNotDefined (tableMap)
  }

  private def makeTableMap(implicit dbsess: Session) : Map[String, MTable] = {
    val tableList = MTable.getTables.list()
    val tableMap = tableList.map{t => (t.name.name, t)}.toMap;
    tableMap;
  }

  private implicit def table2tableInvoker[T <: Table[_]](table: T):DDLTableInvoker = new DDLTableInvoker(table.tableName, table.ddl)

  private class DDLTableInvoker(tableName:String, ddl: DDL){
    def createIfNotDefined(tableMap:Map[String, MTable])(implicit session:Session){
      if(!(tableMap isDefinedAt tableName))
	ddl.create
    }    
  } 
}

