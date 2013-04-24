package books.util

import books.dal._

import play.api.libs.json._
import scala.collection.mutable.ArrayBuffer
import scala.util.parsing.combinator._
import scala.util.parsing.input.{Reader,Position}

import java.io.{StringWriter, FileInputStream, InputStreamReader}

/**
 * Created with IntelliJ IDEA.
 * User: mayleen
 * Date: 19/04/13
 * Time: 14:15
 * To change this template use File | Settings | File Templates.
 */
class EzbParser extends Parsers{
  type Elem = String

  val lineParsers = new LineParser

  def apply(in:LineReader):List[MarkedLine] = {
    phrase(lineToken *)(in) match{
      case Success(mlst, _) =>
        val lines = new ArrayBuffer[MarkedLine]()
        for(ml <- mlst){
          lines.append(ml)
        }
        lines.toList //.mkString("\n")
      case e => throw new IllegalArgumentException("Could not parse " + in + ": " + e)
    }
  }

  def lineToken = Parser{in =>
    if(in.atEnd){
      Failure("Reached End of File", in)
    }else{
      firstChar(in.first) match{
        case '*' => p(lineParsers.sectionTitle)(in)
        case '#' => p(lineParsers.summary)(in)
        case '>' => p(lineParsers.quote)(in)
        case '%' => p(lineParsers.emptyLine)(in)
        case _ => p(lineParsers.normalLine)(in)
      }
    }
  }

  /** Returns the first char in the given string or a newline if the string is empty.
    * This is done to speed up header parsing. Used to speed up line tokenizing substantially
    * by using the first char in a line as lookahead for which parsers to even try.
    */
  def firstChar(line:String):Char = {
    if (line.length == 0) '\n' else line.charAt(0)
  }

  /**
   * Returns a parser based on the given line parser.
   * The resulting parser succeeds if the given line parser consumes the whole String.
   */
  def p[T](parser:lineParsers.Parser[T]):Parser[T] = Parser{in =>
    if (in.atEnd) {
      Failure("End of Input.", in)
    } else {
      lineParsers.parseAll(parser, in.first) match {
        case lineParsers.Success(t, _) => Success(t, in.rest)
        case n:lineParsers.NoSuccess   => Failure(n.msg, in)
      }
    }
  }

}

trait MarkedLine{
  def prefix:String
  def payload:String

  def marker = prefix.charAt(0)

  /** removes all whitespace, nl and trailing hashes from the payload
    * "  foo ##  \n" => "foo"
    */
  def trim() = {
    val s = payload.trim
    var idx = s.length - 1
    while (idx >= 0 && s.charAt(idx) == marker) idx -= 1
    s.substring(0,idx+1).trim
  }

  def level = prefix.length

}

case class Title(val prefix:String, val payload:String) extends MarkedLine{
  override def toString = level match{
    case 1 => s"\"ezb_title\" : \"$payload\""
    case 2 => s"\"part_title\" : \"$payload\""
    case _ => "Undefinded Title Level"
  }
}

case class Summary(val prefix:String, val payload:String) extends MarkedLine{
  override def toString = level match{
    case 1 => s"\"ezb_summary\" : \"$payload\"
    case 2 => s"\"contrib_content\" : \"$payload"
  }
}

case class Quote(val prefix:String, val payload:String) extends MarkedLine{
  override def toString = s"\"contrib_content\" : \"$payload"
}

case class NormalLine(content:String) extends MarkedLine{
  val prefix = ""
  val payload = content

  override def toString = content
}

class EmptyLine extends NormalLine("")

class MarkedLineReader private (val lines:Seq[MarkedLine],
                                 val lineCoutn:Int) extends Reader[MarkedLine]{

  def this(lines:Seq[MarkedLine]) = this(lines, 1)

  private object EofLine

  def first = if (lines.isEmpty) EofLine else lines.head
  def rest  = if (lines.isEmpty) this else new MarkedLineReader(lines.tail, lineCount+1)
  def atEnd = lines.isEmpty
  def pos   = new Position {
    def line   = lineCount
    def column = 1
    protected def lineContents = first.fullLine
  }
}

class LineParser extends RegexParsers{
  /**
   * Matches everything in the parsed string up to the end.
   * Also matches the empty String. Returns the matched String.
   */
  def rest:Parser[String] = Parser { in =>
    if (in.atEnd) {
      Success("", in)
    } else {
      val source = in.source
      val offset = in.offset
      Success(source.subSequence(offset, source.length).toString, in.drop(source.length-offset))
    }
  }

  def lineToken = sectionTitle | summary | emptyLine | quote | normalLine

  /** Parses sections of the form: ** title **
    */
  val sectionTitle:Parser[Title] = """\*+""".r ~ rest ^^ {
    case prefix ~ payload => new Title(prefix, payload)
  }

  val summary:Parser[Summary] = """#+""".r ~ rest ^^ {
    case prefix ~ payload => new Summary(prefix,payload)
  }

  val quote:Parser[Quote] = """>>""".r ~ rest ^^ {
    case prefix ~ payload => new Quote(prefix, payload)
  }

  val normalLine:Parser[NormalLine] = rest ^^ {new NormalLine(_)}

  val emptyLine:Parser[EmptyLine] = rest ^^ {str => new EmptyLine}

}

/**
 * A Reader for reading whole Strings as tokens.
 * Used by the Tokenizer to parse whole lines as one Element.
 */
case class LineReader private (val lines:Seq[String],
                               val lineCount:Int)
  extends Reader[String] {
  /**should never be used anywhere, just a string that should stick out for better debugging*/
  private def eofLine = "EOF"
  def this(ls:Seq[String]) = this(ls, 1)
  def first = if (lines.isEmpty) eofLine else lines.head
  def rest  = if (lines.isEmpty) this else new LineReader(lines.tail, lineCount + 1)
  def atEnd = lines.isEmpty
  def pos   = new Position {
    def line   = lineCount
    def column = 1
    protected def lineContents = first
  }
}

class BlockParser extends Parsers{
  type Elem = MarkedLine

  def appy(in:MarkedLineReader):String = { //TODO or a Json object
    phrase(...)(in) match{
      case Success(blst, _) =>
        //TODO process result
        ""
      case e: NoSuccess => throw new IllegalArgumentException("Could not parse " + in + ": " + e)
    }
  }

  def normalLine:Parser[String] = Parser{
    if(in.atEnd) Failure("End of input reached", in)
    else in.first match{
      case nl:NormalLine => Success(nl.toString)
      case _ => Failure("Not a normal line", in)
    }
  }

  def partTitle:Parser[String] = Parser{in =>
    if(in.atEnd) Failure("End of input reached", in)
    else in.first match{
      case tl:Title if tl.level == 2 => Success(tl.payload, in.rest)
      case _ => Failure("Not a title line", in)
    }
  }

  def ezbTitle:Parser[String] => Parser{in =>
    if(in.atEnd) Failure(in)
    else in.first match{
      case tl:Title if tl.level == 1 => Success(tl.payload, in.rest)
      case _ => Failure(in)
    }
  }

  def summaryLine:Parser[Summary] = Parser{in =>
    if(in.atEnd) Failure("End of input reached", in)
    else in.first match{
      case sl:Summary => Success(sl, in.rest)
      case _ => Failure("Not a summary line", in)
    }
  }

  def ezoombook:Parser[String] = ezbTitle ~ rest ^^ {
    case title ~ rst => s"""{"type" : "ezoombook", "title" : \"$title\", $rst }"""
  }

  def partBlock:Parser[String] = partTitle ~ rest ^^ {
    case title ~ rst => s"""{"type" : "part", "part_title" : \"$title\", $rst }"""
  }

  def summaryBlock:Parser[String] = summaryLine ~ (normalLine*) ^^ {
    case first ~ rest => s"""{"type" : "contrib.Summary", "content" : ${rest.flatten} }"""
  }


}

object Transformer{
  val parser = new EzbParser()

  def apply(in:Seq[String]):String = {
    val lineReader = new LineReader(in)
    val parsedLines = parser(lineReader)
    //parsedLines.mkString("\n")
    createEzoombook(parsedLines)
  }

  def createEzoombook(lines:List[MarkedLine]) = {
    val out = new StringBuilder("""{"type":"ezoombook"""")

//    lines.foreach{
//      case t:Title if t.level==1 => out.append(s"\"title\":\"$t.payload\"")
//      case t:Title if t.level==2 => out.append(s"\"title\":\"$t.payload\"")
//      case _ =>
//    }

//     out.append("}")
//     out.toString

    lines.foldLeft("""{"type":"ezoombook,""""){(str,ln) => str + processLine(ln)}
  }

  def processLine(ln:MarkedLine):String = ln match{
    case t:Title if t.level==1 => s""" "title":"${t.payload},""""
    case t:Title if t.level==2 => s"""{title":"${t.payload},""""
    case _ => ""
  }
}

