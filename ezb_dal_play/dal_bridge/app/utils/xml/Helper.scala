package utils.xml

import scala.xml._

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 23/08/13
 * Time: 15:11
 * To change this template use File | Settings | File Templates.
 */
object Helper {

  // A copy-friendly Attribute
  case class GenAttr(pre:Option[String], key:String, value:Seq[Node], next:MetaData){
    def toMetaData:MetaData = Attribute(pre, key, value, next)
  }

  // Converts scala.xml Attributes to GenAttr
  def decomposeMetaData(m:MetaData):Option[GenAttr] = m match{
    case PrefixedAttribute(pre, key, value, next) =>
      Some(GenAttr(Some(pre), key, value, next))
    case UnprefixedAttribute(key, value, next) =>
      Some(GenAttr(None, key, value, next))
    case _ => None
  }

  // Converts chain of MetaData to Iteragle GenAttr
  def unchainMetaData(m: MetaData): Iterable[GenAttr] = {
    m flatMap (decomposeMetaData)
  }

  // Converts iterable of GenAttr to chain of MetaData
  def chainMetaData(l: Iterable[GenAttr]): MetaData = l match {
    case Nil => Null
    case head :: tail => head.copy(next = chainMetaData(tail)).toMetaData
  }

  // Allows to apply a mapping to an attribute chain
  def mapMetaData(m: MetaData)(f: GenAttr => GenAttr): MetaData =
    chainMetaData(unchainMetaData(m).map(f))

  // Applies the partial function pf to a node n and all its children,
  // returning the transformed node
  def transform(n:Node)(pf: PartialFunction[Node,Node]): Node =
    transformIf(n, pf.isDefinedAt(_), pf.apply(_))

  private def transformIf(n:Node, pred: (Node) => Boolean, trans:(Node)=> Node): Node =
    (if (pred(n)) trans(n) else n) match{
      case e:Elem => e.copy(child = e.child.map(transformIf(_, pred, trans)))
      case other => other
    }
}
