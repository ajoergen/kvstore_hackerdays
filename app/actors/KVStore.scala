package actors

import akka.actor.{Actor, ActorRef, Props}

object KVStore {
  case object Join
  case object Joined

  case class Replicas(replicas: Set[ActorRef])
  case class Put(key: String, value: Long)
  case class Ack(id: Long, key: String, value: Long)

  def props(n: Int) = Props(new KVStore(n))
}

class KVStore(n: Int) extends Actor {
  import actors.KVStore._
  var replicas = Set.empty[ActorRef]
  val idSequence = generateId(0).iterator
  var pendingAcks = Map.empty[Long, (String, Long)]

  override def receive: Receive = {
    case Join =>
      val replica = sender()
      replicas += replica
      replicas foreach( _ ! Replicas(replicas))
      replica ! Joined
  }

  private def generateId(n: Long): Stream[Long] = Stream.cons(n, generateId(n+1))
}
