package actors

import actors.Replica.Update
import akka.actor.{Props, Actor, ActorRef}

object KVStore {
  case object Join
  case object Joined

  case class Replicas(replicas: Set[ActorRef])
  case class Put(key: String, value: Long)
  case class Ack(id: Long, key: String, value: Long)

  def props(n: Int) = Props(classOf[KVStore], new KVStore(n))
}

class KVStore(n: Int) extends Actor {
  import actors.KVStore._
  var replicas = Set.empty[ActorRef]
  val idSequence = generateId(0).iterator
  var pendingAcks = Map.empty[Long, (String, Long)]

  override def preStart(): Unit = {
    for (i <- 0 until n) Replica.props(self, Persistence.props(false))
  }

  override def receive: Receive = {
    case Join =>
      val replica = sender()
      replicas += replica
      replicas foreach( _ ! Replicas(replicas))
      replica ! Joined
  }

  private def generateId(n: Long): Stream[Long] = Stream.cons(n, generateId(n+1))
}
