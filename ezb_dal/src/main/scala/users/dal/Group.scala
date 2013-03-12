package users.dal

import java.util.UUID

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 11/23/12
 * Time: 9:47 PM
 * To change this template use File | Settings | File Templates.
 */

case class Group(groupId:UUID, name:String, ownerId:UUID)

trait GroupComponent{
  this: (Profile with UserComponent) =>

  import profile.simple._

  object Groups extends Table[Group]("groups"){
    def id = column[UUID]("group_id", O.PrimaryKey)
    def name = column[String]("group_name", O.NotNull)
    def ownerId = column[UUID]("group_owner", O.NotNull)
    def * = id ~ name ~ ownerId <> (Group, Group.unapply _ )
   
    def owner = foreignKey("group_owner", ownerId, Groups)(_.id)

    def add(group_name:String, owner_id:UUID)(implicit session:Session) = {
      Groups.insert(new Group(UUID.randomUUID(), group_name, owner_id))
    }

    def getUserGroups(owner_id:UUID)(implicit session:Session) = {
      Query(Groups).filter(_.ownerId === owner_id.bind).list
    }

  }

  object Roles extends Enumeration{
    val owner, coordinator, collaborator = Value
  }

  implicit val rolesTypeMapper = MappedTypeMapper.base[Roles.Value, Int](_.id, Roles(_))

  object GroupMembers extends Table[(UUID,UUID,Roles.Value)]("group_members"){
    def groupId = column[UUID]("group_id")
    def userId = column[UUID]("user_id")
    def userRole = column[Roles.Value]("group_member_role")
    def * = groupId ~ userId ~ userRole
    
    def pk = primaryKey("pk_group_member", (groupId, userId))

    def member = foreignKey("user_id", userId, Users)(_.id)
    def group = foreignKey("group_id", groupId, Groups)(_.id)


    def getGroupMembers(group_id:UUID)(implicit session:Session) = {
      Query(GroupMembers).filter(_.groupId === group_id.bind).map(_.userId).list
    }

//    def getGroupsByMember(member_id:UUID)(implicit session:Session) = {
//      
//    }
  }
}
