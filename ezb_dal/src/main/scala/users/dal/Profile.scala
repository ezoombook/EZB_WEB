package users.dal

import slick.driver.ExtendedProfile

/**
 * Created with IntelliJ IDEA.
 * User: gonto
 * Date: 11/23/12
 * Time: 11:32 PM
 * To change this template use File | Settings | File Templates.
 */

/**
 * Trait to be combined with all DAL components to add session support.
 */
trait Profile {
  val profile: ExtendedProfile
}
