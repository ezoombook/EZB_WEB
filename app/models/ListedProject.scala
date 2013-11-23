package models

import java.util.UUID

case class ListedProject(
  projId:UUID,
  projName:String,
  projOwner:UUID,
  projectCreationDate:Long,
  ezbId:Option[UUID],
  ezbTitle:String
)
