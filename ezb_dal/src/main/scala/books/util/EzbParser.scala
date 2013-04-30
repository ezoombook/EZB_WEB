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
  def trim() = if (!empty){
    val s = payload.trim
    var idx = s.length - 1
    while (idx >= 0 && s.charAt(idx) == marker) idx -= 1
    s.substring(0,idx+1).trim
  } else { "" }

  def level = prefix.length

  def empty:Boolean = prefix.isEmpty && payload.isEmpty
}

case class Title(val prefix:String, val payload:String) extends MarkedLine{
  override def trim = super.trim.filterNot(_ == '"')
//    level match{
//    case 1 => s""" "ezb_title" : "$payload" """
//    case 2 => s""" "part_title" : "$payload" """
//    case _ => "Undefinded Title Level"
//  }
}

case class Summary(val prefix:String, val payload:String) extends MarkedLine{
  override def trim = if (!empty){ super.trim.replaceAll("\"", "'") } else ""
  override def toString = trim
}

case class Quote(val prefix:String, val payload:String) extends MarkedLine{
  override def trim = if(!empty){ super.trim.replaceAll("\"", "'")} else ""
  override def toString = trim
}

case class NormalLine(content:String) extends MarkedLine{
  val prefix = ""
  val payload = content

  override def toString = if (!empty){ content.replaceAll("\"", "'") } else ""
}

class EmptyLine extends NormalLine("")

class MarkedLineReader private (val lines:Seq[MarkedLine],
                                 val lineCount:Int) extends Reader[MarkedLine]{

  def this(lines:Seq[MarkedLine]) = this(lines, 1)

  private object EofLine extends NormalLine("\nEOF\n")

  def first = if (lines.isEmpty) EofLine else lines.head
  def rest  = if (lines.isEmpty) this else new MarkedLineReader(lines.tail, lineCount+1)
  def atEnd = lines.isEmpty
  def pos   = new Position {
    def line   = lineCount
    def column = 1
    protected def lineContents = first.payload
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

  def apply(in:MarkedLineReader):Either[String, JsValue] = {
    phrase(ezoombook)(in) match{
      case Success(blst, _) =>
        Right(Json.parse(blst.mkString))
      case e: NoSuccess => Left("Could not parse " + in + ": " + e)
    }
  }

  def normalLine:Parser[String] = Parser{in =>
    if(in.atEnd) Failure("End of input reached", in)
    else in.first match{
      case nl:NormalLine => Success(nl.toString, in.rest)
      case _ => Failure("Not a normal line", in)
    }
  }

  def emptyLine:Parser[String] = Parser{in =>
    if(in.atEnd) Failure("End of input reached", in)
    else in.first match{
      case nl:NormalLine if nl.empty => Success(nl.toString, in.rest)
      case _ => Failure("Not an empty line", in)
    }
  }

  def partTitle:Parser[String] = Parser{in =>
    if(in.atEnd) Failure("End of input reached", in)
    else in.first match{
      case tl:Title if tl.level == 2 => Success(tl.trim, in.rest)
      case _ => Failure("Not a part title line", in)
    }
  }

  def ezbTitle:Parser[String] = Parser{in =>
    if(in.atEnd) Failure("End of input", in)
    else in.first match{
      case tl:Title if tl.level == 1 => Success(tl.trim, in.rest)
      case _ => Failure("End of input", in)
    }
  }

  def bookSummaryLine:Parser[String] = Parser{in =>
    if(in.atEnd) Failure("End of input reached", in)
    else in.first match{
      case sl:Summary if sl.level ==1 => Success(sl.trim, in.rest)
      case _ => Failure("Not a summary line", in)
    }
  }

  def contribSummaryLine:Parser[String] = Parser{in =>
    if(in.atEnd) Failure("End of input reached", in)
    else in.first match{
      case sl:Summary if sl.level == 2 => Success(sl.trim, in.rest)
      case _ => Failure("Not a summary line", in)
    }
  }

  def quoteLine:Parser[Quote] = Parser{in =>
    if(in.atEnd) Failure("End of input reached", in)
    else in.first match{
      case sl:Quote => Success(sl, in.rest)
      case _ => Failure("Not a quote line", in)
    }
  }

  def ezoombook:Parser[String] = ezbTitle ~ ((emptyLine*) ~> (bookSummary*)) ~ (contrib+) ^^ {
    case title ~ bsumm ~ rst =>
      s"""{"type" : "ezoomlayer",
      "ezoombook_title" : \"$title\",
      "ezoomlayer_summaries" : [${bsumm.mkString(",")}],
      "ezoomlayer_contribs" : [${rst.mkString(",")}] }"""
    case title ~ rsr =>  ""
  }

  def bookSummary = bookSummaryLine ~ (normalLine*) ^^ {
    case sumtitle ~ rest =>
      s""" "${sumtitle.trim} ${rest.mkString}" """
  }

  def contrib = partBlock | summaryBlock

  def partBlock:Parser[String] = partTitle ~ (partContrib*) ^^ {
    case title ~ rst =>
      s"""{"contrib_type" : "part", "part_title" : \"$title\",
           "part_contribs" : [${rst.mkString(",")}] }"""
  }

  def partContrib = quoteBlock | summaryBlock

  def summaryBlock:Parser[String] = contribSummaryLine ~ (normalLine*) ^^ {
    case first ~ rest =>
      s"""{"contrib_type" : "contrib.Summary",
           "contrib_content" : "$first ${rest.mkString}" }"""
  }

  def quoteBlock:Parser[String] = quoteLine ~ (normalLine*) ^^ {
    case first ~ rest =>
      s"""{"contrib_type" : "contrib.Summary",
           "contrib_content" : "$first ${rest.mkString}" }"""
  }

  def rest = (summaryBlock*) | (partBlock*) | (quoteBlock*)
}

/**
 * Transformed a marked-down-ish text into a Json object
 */
object Transformer{
  val lineParser = new EzbParser()
  val blockParser = new BlockParser()

  def apply(in:Seq[String]):Either[String,JsValue] = {
    val lineReader = new LineReader(in)
    val parsedLines = lineParser(lineReader)

    val blockReader = new MarkedLineReader(parsedLines)
    blockParser(blockReader)
  }
}

