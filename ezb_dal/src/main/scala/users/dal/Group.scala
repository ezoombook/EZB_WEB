package users.dal

import java.util.UUID

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 11/23/12
 * Time: 9:47 PM
 * To change this template use File | Settings | File Templates.
 */

/**
 * The Group entitiy
 */ 
case class Group(id:UUID, name:String, ownerId:UUID)

/**
 * A component to manage the Groups and Group-Members tables
 */ 
trait GroupComponent{
  this: (Profile with UserComponent) =>

  import profile.simple._

  /**
   * The Groups table object
   */ 
  object Groups extends Table[Group]("groups"){
    def id = column[UUID]("group_id", O.PrimaryKey, O.DBType("UUID"))
    def name = column[String]("group_name", O.NotNull)
    def ownerId = column[UUID]("group_owner", O.NotNull, O.DBType("UUID"))
    def * = id ~ name ~ ownerId <> (Group, Group.unapply _ )
   
    def owner = foreignKey("fk_group_owner", ownerId, Users)(_.id)

    def add(group_name:String, owner_id:UUID)(implicit session:Session) = {
      Groups.insert(new Group(UUID.randomUUID(), group_name, owner_id))
    }

    /**
     * Returns the groups owned by a user
     */ 
    def getUserGroups(owner_id:UUID)(implicit session:Session) = {
      Query(Groups).filter(_.ownerId === owner_id.bind).list
    }

    /**
     * Returns the group identified by the id @param group_id
     */ 
    def getGroup(group_id:UUID)(implicit session:Session) = {
      Query(Groups).filter(_.id === group_id.bind).firstOption
    }
  }

  /**
   * An enumeration of the roles users can have in a group
   */ 
  object Roles extends Enumeration{
    val owner, coordinator, collaborator = Value
  }

  implicit val rolesTypeMapper = MappedTypeMapper.base[Roles.Value, Int](_.id, Roles(_))

  /**
   * The Group-Members table object
   */ 
  object GroupMembers extends Table[(UUID,UUID,Roles.Value)]("group_members"){
    def groupId = column[UUID]("group_id", O.DBType("UUID"))
    def userId = column[UUID]("user_id", O.NotNull, O.DBType("UUID"))
    def userRole = column[Roles.Value]("group_member_role")
    def * = groupId ~ userId ~ userRole
    
    def pk = primaryKey("pk_group_member", (groupId, userId))

    def member = foreignKey("fk_user_id", userId, Users)(_.id)
    def group = foreignKey("fk_group_id", groupId, Groups)(_.id)

    def addMember(group_id:UUID, member_id:UUID, member_role:String)(implicit session:Session) = {
      GroupMembers.insert(group_id, member_id, Roles.withName(member_role))
    }

    def changeMemberRole(group_id:UUID, member_id:UUID, new_role:Roles.Value)(implicit session:Session) = {
      val mquery = GroupMembers.filter(gm => gm.groupId === group_id.bind && gm.userId === member_id.bind).map(_.userRole)
      mquery.update(new_role)
    }

    def getGroupMembers(group_id:UUID)(implicit session:Session) = 
      (for {
	(g, u) <- GroupMembers innerJoin Users on (_.userId === _.id)
	if g.groupId === group_id.bind
      } yield (u)).list
    

    /**
     * Returns a list of groups where a user belongs.
     */ 
    def getGroupsByMember(member_id:UUID)(implicit session:Session) = {
      Query(GroupMembers).filter(_.userId == member_id.bind).map(_.groupId).list
    }
  }
}
