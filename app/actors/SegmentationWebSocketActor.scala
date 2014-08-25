package actors

import akka.actor._
import play.api.cache.Cache
import service.ProbabilityDistribution
import play.api.Play.current

trait Segmentation {
  def splits(text: String, maxLength: Int = 20) =
    (1 to math.min(text.length, maxLength)).map(i => (text.substring(0, i), text.substring(i, text.length)))
}

trait OneGramTextSegmentation extends Segmentation {
  val dist: ProbabilityDistribution

  def segment(word: String): List[String] = {
    Cache.get(word) match {
      case None =>
        word match {
          case "" => Nil
          case s: String =>
            val result = splits(s).map {
              case (first,reminder) => first::segment(reminder)
            }.maxBy(a => a.foldLeft(1.0) {(x,y) =>
              x*dist(y)
            })
            Cache.set(s, result)
            result
        }
      case Some(x) => x.asInstanceOf[List[String]]
    }
  }
}

trait BiGramTextSegmentation extends Segmentation {
  val dist: ProbabilityDistribution
  val dist2w: ProbabilityDistribution

  def segment(word: String, predecessor: String = "<S>"): (Double, List[String]) = {
    val bigram = "%s %s".format(predecessor, word)
    Cache.get(bigram) match {
      case None =>
        word match {
          case "" => (0.0, Nil)
          case w: String =>
            val result = splits(word).map {
              case (head,tail) => combine(math.log10(conditionalProbability(head, predecessor)), head,segment(tail, head))
            }.maxBy(x => x._1)
            Cache.set(bigram, result)
            result
        }
      case Some(v) => v.asInstanceOf[(Double, List[String])]
    }
  }

  def combine(pHead: Double, head: String, tail: (Double, List[String])): (Double,List[String]) = {
    (pHead+tail._1, List(head) ++ tail._2)
  }

  // P(word|prev) => Close to ... This is not quite a probability distribution, since the can sum > 1.
  // The technique is called stupid back off and works well in practice in robotics. It seems to do the job here as well.
  def conditionalProbability(word: String, prev: String) = {
    val bigram = prev + ' ' + word
    dist2w(bigram) match {
      case 9.75697076484373E-13 => dist(word)
      case x =>
        if (dist.count(prev) == 0) dist(word)
        else dist2w.count(bigram).toDouble/dist.count(prev).toDouble
    }
  }
}

/**
 * WebSocket actor receiving events for the segmentation service.
 */
object SegmentationWebSocketActor {
  val dist = new ProbabilityDistribution("/home/anjj/Projects/play/NGrams/public/resources/count_1w.txt", BigInt("1024908267229"), (w: String, n:BigInt) => 10./(n.toDouble*math.pow(10,w.length)))
  val dist2w = new ProbabilityDistribution("/home/anjj/Projects/play/NGrams/public/resources/count_2w.txt", BigInt("1024908267229"))

  def props(out: ActorRef) = {
    Props(new SegmentationWebSocketActor(out, dist, dist2w))
  }
}

class SegmentationWebSocketActor(out: ActorRef, val dist: ProbabilityDistribution, val dist2w: ProbabilityDistribution) extends Actor with BiGramTextSegmentation {
  val numberOfTokens = BigInt("1024908267229")

  def receive = {
    case msg: String =>
      out ! segment(msg)._2.mkString(" ")
  }
}
