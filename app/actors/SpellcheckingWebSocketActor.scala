package actors

import akka.actor.{ActorLogging, Props, ActorRef, Actor}
import service.ProbabilityDistribution
import play.api.cache.Cache
import play.api.Play.current
import scala.collection.mutable
import scala.language.implicitConversions

object SpellcheckingWebSocketActor {
  val pdWord = new ProbabilityDistribution("/home/anjj/Projects/play/NGrams/public/resources/count_1w.txt", BigInt("1024908267229"), (w: String, n:BigInt) => 10./(n.toDouble*math.pow(10,w.length)))
  val pdEdit = new ProbabilityDistribution("/home/anjj/Projects/play/NGrams/public/resources/count_1edit.txt")
  val prefixes = (for {
    w <- pdWord.keySet
    i <- 0 to w.size} yield w.slice(0,i)).toSet

  def props(out: ActorRef) = Props(new SpellcheckingWebSocketActor(out))
  implicit def char2string(c: Char): String = c.toString
}

class SpellcheckingWebSocketActor(out: ActorRef) extends Actor with ActorLogging {
  import SpellcheckingWebSocketActor._

  val P_SPELL_ERROR = 0.75 // In the real world this parameter could be learned and set to the optimal value.

  /**
   * Yields the the probability of an edit
   * @param edit The edit sequence that can be one of "", "a|b" or "a|b+c|d"
   * @return The probability as given by the probability distribution
   */
  def pEdit(edit:String) = {
    edit match {
      case "" => 1.0-P_SPELL_ERROR
      case ed: String =>
        ed.split("\\+").foldLeft(1.0) { (runningResult,currentValue) =>
          runningResult*pdEdit(currentValue)
        }
    }
  }

  def addEdit(left: String, right: String, edits: List[String]) = edits ::: List("%s|%s".format(right, left))

  def updateEditsMap(head: String, tail: String, edits: List[String], editsMap: mutable.Map[String, String])  {
    val candidate = head + tail
    pdWord.count(candidate) match {
      case 0 => Unit
      case _ =>
        val e = edits.mkString("+")
        editsMap.get(candidate) match {
          case None =>
            editsMap update(candidate, e)
          case Some(c) =>
            // We have already seen the candidate, pick the most likely edit sequence?
            editsMap update(candidate, List(c,e).maxBy(str => pEdit(str)))
        }
    }
  }

  def generateEdits(head: String, tail: String, d: Int, edits: List[String], editsMap: mutable.Map[String, String]) {
    updateEditsMap(head, tail, edits, editsMap)
    d match {
      case 0 => Unit // Number of edits reached stop the recursion.
      case _ =>
        val extensions = ('a' to 'z') map(head + _) filter prefixes.contains
        val prevChar = head match {
          case "" => '<'
          case _ => head.last
        }
        // Handle inserts
        extensions.foreach { h =>
          generateEdits(h, tail, d - 1, addEdit(prevChar.toString + h.last, prevChar, edits), editsMap)
        }
        tail match {
          case "" => Unit // No more tail to process. Stop the recursion.
          case _ =>
            // Deletion
            generateEdits(head, tail.substring(1), d-1, addEdit(prevChar, prevChar+tail(0).toString, edits), editsMap)
            extensions.foreach { h =>
              if (h.last == tail(0)) generateEdits(h, tail.substring(1), d, edits, editsMap) // Match means edit next character
              else generateEdits(h, tail.substring(1), d-1, addEdit(h.last, tail.head, edits), editsMap) // Replace the character at tail.head
              // editsR(h, tl[1:], d-1, ed(h[-1], tl[0]))
            }
            //Transpose - We save the hard one to last :-( swap the first two characters in tail and recurs
            if (tail.length >= 2 && tail(0) != tail(1) && prefixes.contains(head + tail(1)))
              generateEdits(head+tail(1), tail(0).toString + tail.substring(2), d-1, addEdit(tail(1).toString + tail(0), tail.substring(0,2), edits), editsMap)
        }
    }
  }

  // According to the literature 85-90% of errors are within an edit distance of one, but I seem to need 2 to get close to those numbers.
  def edits(word: String, d: Int = 2): mutable.Map[String, String] = {
    Cache.get(word) match {
      case None =>
        val result = mutable.OpenHashMap.empty[String,String] // We try OpenHashMap https://gist.github.com/pchiusano/1423303
        generateEdits("", word, d, List(), result)
        Cache.set(word, result)
        log.debug("%s".format(result.map(p => (pEdit(p._2)*pdWord(p._1),p._1, p._2)).toSeq.sortBy(p => -p._1).take(5)))
        result
      case Some(x) => x.asInstanceOf[mutable.Map[String,String]]
    }
  }

  def correctAll(message: String): List[String] = {
    val regExp = """\w+""".r
    regExp.findAllIn(message).map { w =>
      edits(w) match {
        case m: mutable.Map[String,String] if m.isEmpty => "???"
        case m: mutable.Map[String,String] => m.maxBy(p => pEdit(p._2)*pdWord(p._1))._1
        case _ => throw new IllegalArgumentException("The type return by edits is not a mutable map")
      }
    }.toList
  }

  def receive = {
    case msg: String =>
      val result = correctAll(msg)
      out ! result.mkString(" ")
  }
}
