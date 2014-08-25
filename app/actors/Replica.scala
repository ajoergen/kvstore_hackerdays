package actors

import actors.KVStore.{Replicas, Join}
import actors.Persistence.{Persisted, Persist}
import akka.actor.{Props, ActorRef, Actor}
import scala.concurrent.duration._

object Replica {

  case class Entry(key: String, value: Long)
  case class Get(key: String)
  case class Update(id: Long, key: String, value: Long)
  case class Updated(key: String)
  case class Replicate(id: String, entry: Entry)
  case class Replicated(id: String, key: String)

  def props(sentinel: ActorRef, persistenceProps: Props): Props = Props(new Replica(sentinel, persistenceProps))
}

class Replica(val sentinel: ActorRef, val persistenceProps: Props, resendCount: Int = 5) extends Actor {
  import actors.Replica._
  import context.dispatcher
  import scala.language.postfixOps

  var cache = Map.empty[String, Long]
  var pendingPersistenceAcks = Set.empty[(Long, Entry)]
  var resendCounts = Map.empty[Long, Int]

  var replicas = Set.empty[ActorRef]

  val persistence = context.actorOf(persistenceProps)

  val persistenceRetryJob = context.system.scheduler.schedule(100 milliseconds, 100 milliseconds, new Runnable() {
    override def run(): Unit = pendingPersistenceAcks.foreach { t =>
      val (id, entry) = (t._1, t._2)
      resendCounts.get(id) match {
        case None => throw new RuntimeException("WTF!")
        case Some(v) =>
          if (v < resendCount) {
            persistence  ! Persist(id, entry)
            resendCounts.updated(id, v + 1)
          } else {
            throw new RuntimeException("Perstence of %d failed %d times".format(id, resendCount))
          }
      }
    }
  })

  override def preStart(): Unit = {
    sentinel ! Join
  }

  override def receive: Receive = {
    case Replicas(newReplicas) => replicas = newReplicas
    case Get(x) => sender() ! cache.get(x)
    case Update(id, k,v) =>
      cache.updated(k,v)
      // replcate k,v to everyone
      persistence ! Persist(id, Entry(k,v))
      val t = (id,Entry(k,v))
      pendingPersistenceAcks += (t)
    case Persisted(id, e) =>
      val t = (id, e)
      pendingPersistenceAcks -= (t)
  }


}
