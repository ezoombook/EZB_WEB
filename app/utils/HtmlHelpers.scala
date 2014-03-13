
import views.html.helper.FieldConstructor
import views.html._

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 14/11/2013
 * Time: 14:31
 * To change this template use File | Settings | File Templates.
 */
package utils.ezbhelpers{

object EzbHelpers {
  implicit val myfields = FieldConstructor(ezbhelpers.ezlayerFieldTemplate.f)
}

object navHelpers {
  implicit val myfields = FieldConstructor(ezbhelpers.navTemplate.f)
}

object bookFieldHelper{
  implicit val bookfields = FieldConstructor(ezbhelpers.bookFieldTemplate.f)
}

object bootstrapHelper{
  implicit val formFields = FieldConstructor(ezbhelpers.bootstrap3template.f)
}

object formHelper{
  def completeIndexes(field: play.api.data.Field, min:Int = 1):Seq[Int] = field.indexes match {
    case Nil => 0 until min
    case complete if complete.size >= min => field.indexes
    case partial =>
      // We don't have enough elements, append indexes starting from the largest
      val start = field.indexes.max + 1
      val needed = min - field.indexes.size
      field.indexes ++ (start until (start + needed))
  }
}

}