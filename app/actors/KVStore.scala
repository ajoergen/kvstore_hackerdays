package actors

import actors.Replica.{OperationAck, GetResult, Update, GetValue}
import akka.actor.{Actor, ActorRef, Props}

import scala.collection.immutable.TreeMap

object KVStore {
  case object Join
  case object Joined

  case class Replicas(replicas: Set[ActorRef])
  case class Get(key: String)
  case class Put(key: String, value: Long)
  case class Ack(id: Long, key: String, value: Long)

  def props(n: Int) = Props(new KVStore(n))
}

class KVStore(n: Int) extends Actor {
  import actors.KVStore._
  var replicas = Set.empty[ActorRef]
  val idSequence = Iterator.iterate(0L) { case x => x + 1}
  var pendingAcks = Map.empty[Long, (String, Long)]
  var constistenHash: TreeMap[Long, ActorRef] = TreeMap.empty[Long, ActorRef]

  def findReplica(key: String): ActorRef = {

  }

  override def receive: Receive = {
    case Join =>
      val replica = sender()
      replicas += replica
      replicas foreach( _ ! Replicas(replicas))
      replica ! Joined
    case Get(key) =>
      val replica = findReplica(key)
      replica ! GetValue(idSequence.next(), key)
    case GetResult(id, key, valueOption) =>
      valueOption match {
        case Some(x) =>  // Send result
        case None => // Hmm...
      }
    case Put(key, value) =>
      val replica = findReplica(key)
      replica ! Update(idSequence.next(), key, value)
    case OperationAck(id) =>
      // Log
  }
}
