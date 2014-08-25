package service

import scala.io.Source

/**
 * A probability distribution estimated from counts in a datafile.
 * @param dataFileName The filename containing counts of words from different sources.
 * @param numberOfTokens The total number of tokens
 * @param missingTokenFunction The function determining the probability of words not in the datafile.
 */
class ProbabilityDistribution(dataFileName: String, var numberOfTokens: BigInt = null, missingTokenFunction: (String, BigInt)=>Double = (k,N) => 1.0/N.toDouble) {

  private val distribution= Map.empty[String, Long]
  Source.fromFile(dataFileName, "iso8859-15").getLines().map(_.split("\t")).foreach(a => updateDistribution(a(0), a(1).toLong))
  //if (numberOfTokens == null) numberOfTokens = BigInt(distribution.values.sum)

  def apply(key: String): Double = {
//    distribution.get(key) match {
//      case None => missingTokenFunction(key,   numberOfTokens)
//      case Some(x) => 1.0*x/numberOfTokens.toDouble
//    }
    ???
  }

  def count(key: String): Long = {
//    distribution.get(key) match {
//      case None => 0L
//      case Some(x) => x
//    }
    ???
  }

  def missingToken(key: String) = missingTokenFunction(key,numberOfTokens)

  def keySet = Set.empty[String]

//  override def toString = distribution.foldLeft("")((a,b) => a + "Key: %s, Value: %d\n".format(b._1, b._2))

  private def updateDistribution(word: String, count: Long) {
//    distribution.get(word) match {
//      case None => distribution.update(word, count)
//      case Some(c)  => distribution.update(word, c + count)
//    }
  }


  def mySplit(head: String, tail: String): (String, String) = {
    tail.headOption match {
      case None => (head, tail)
      case Some(c) =>
        c match {
          case '\t' => (head, tail.tail)
          case _ => mySplit(head+tail.head, tail.tail)
        }
    }
  }
}
