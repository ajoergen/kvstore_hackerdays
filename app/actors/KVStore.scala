package actors

import actors.Replica.{OperationAck, GetResult, Update, GetValue}
import akka.actor.{Actor, ActorRef, Props}

import scala.collection.immutable.TreeMap

object KVStore {
  case object Join
  case object Joined

  case class FindReplica(key: String)
  case class Replicas(replicas: Set[ActorRef])
  case class Get(key: String)
  case class Put(key: String, value: Long)
  case class Ack(id: Long, key: String, value: Long)

  def props(n: Int) = Props(new KVStore(n, (s) => s.hashCode))
}

class KVStore(n: Int , hashFunction : (String) => Int) extends Actor {
  import actors.KVStore._
  var replicas = Set.empty[ActorRef]
  val idSequence = Iterator.iterate(0L) { case x => x + 1}
  var pendingAcks = Map.empty[Long, (String, Long)]
  var consistentHash: TreeMap[Int, ActorRef] = TreeMap.empty[Int, ActorRef]

  def findReplica(key: String): ActorRef = {
    val hash = hashFunction(key)
    val tail = consistentHash.drop(hash)
    if (tail.isEmpty ) consistentHash.head._2  else tail.head._2
  }

  override def receive: Receive = {
    case Join =>
      val replica = sender()
      replicas += replica
      consistentHash += hashFunction(replica.toString()) -> replica
      replicas foreach( _ ! Replicas(replicas))
      replica ! Joined
    case Get(key) =>
      val replica = findReplica(key)
      replica ! GetValue(idSequence.next(), key, sender())

    case GetResult(id, key, valueOption, replyTo) =>
      valueOption match {
        case Some(x) =>  replyTo ! valueOption // Send result
        case None => // Hmm...
      }
    case Put(key, value) =>
      val replica = findReplica(key)
      replica ! Update(idSequence.next(), key, value)
    case OperationAck(id) =>
      // Log
    case FindReplica(key) =>
      sender() ! findReplica(key)

  }
}
