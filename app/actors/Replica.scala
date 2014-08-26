package actors

import actors.KVStore.{Replicas, Join}
import actors.Persistence.Persisted
import akka.actor.{ActorLogging, Props, ActorRef, Actor}
import scala.concurrent.duration._

object Replica {
  sealed trait Operation {
    val id: Long
    val key: String
  }
  case class GetValue(id: Long, key: String, replyTo : ActorRef) extends Operation
  case class Update(id: Long, key: String, value: Long) extends Operation
  case class Remove(id: Long, key: String) extends Operation
  case class Replicate(id: Long, key: String, value: Option[Long]) extends Operation
  case class Persist(id: Long, key: String, valueOption: Option[Long]) extends Operation

  sealed trait AckOperation {
    val origin: ActorRef
    val target: ActorRef
    val operation: Operation
  }
  case class Ack(id: Long, key: String, value: Option[Long], origin: ActorRef, target: ActorRef, operation: Operation) extends AckOperation

  sealed trait OperationReply
  case class GetResult(id: Long, key: String, valueOption: Option[Long], replyTo: ActorRef) extends OperationReply
  case class OperationAck(id: Long) extends OperationReply
  case class OperationFailed(id: Long) extends OperationReply

  case class Replicated(id: Long, key: String, valueOption: Option[Long])
  case class CheckAckFailed(id: Long, key: String, target: ActorRef)
  def props(sentinel: ActorRef, persistenceProps: Props): Props = Props(new Replica(sentinel, persistenceProps))
}

class Replica(val sentinel: ActorRef, val persistenceProps: Props) extends Actor with ActorLogging {
  import actors.Replica._
  import context.dispatcher
  import scala.language.postfixOps

  var cache = Map.empty[String, Long]
  //var pendingPersistenceAcks = Set.empty[(Long, Entry)]
  var resendCounts = Map.empty[Long, Int]

  var replicas = Set.empty[ActorRef]

  val persistence = context.actorOf(persistenceProps)

  var pendingAcks = Map.empty[(Long,String,ActorRef), Ack]

  val persistenceRetryJob = context.system.scheduler.schedule(100 milliseconds, 100 milliseconds, new Runnable() {
    override def run(): Unit =
      pendingAcks.foreach {
        case (_, Ack(_,_,_,_,target, operation)) =>
          target ! operation
        case _ => // TODO: Maybe
      }
  })

  override def preStart(): Unit = {
    sentinel ! Join
  }

  def updateKeyValueStore(key: String, valueOption: Option[Long]) = {
    valueOption match {
      case Some(v) => cache += (key -> v)
      case None => cache -= key
    }
  }

  private def replicateNewValue(id: Long, key: String, valueOption: Option[Long], origin: ActorRef) = {
    replicas foreach {replica =>
      val r = Replicate(id, key, valueOption)
      replica ! r
      pendingAcks += ((id, key, replica) -> Ack(id, key, valueOption, origin, replica, r))
      checkForFailureEventually(id, key, replica)
    }
  }

  def persistNewValue(id: Long, key: String, valueOption: Option[Long], origin: ActorRef) = {
    val p = Persist(id, key, valueOption)
    persistence ! p
    pendingAcks += ((id, key, persistence) -> Ack(id, key, valueOption, origin, persistence, p))
    checkForFailureEventually(id, key, persistence)

  }

  override def receive: Receive = handleKeyStoreOperations orElse {
    case Replicas(newReplicas) =>
      replicas = newReplicas - self
    case Replicate(id, key, valueOption) =>
      updateKeyValueStore(key, valueOption)
      sender() ! Replicated(id, key, valueOption)

    case Replicated(id, key, valueOption) =>
      pendingAcks.get((id, key, sender())) match {
        case Some(Ack(id, key, _, origin, _,_)) =>
          val t = (id, key, sender())
          pendingAcks -= t
        case None => // TODO: Haven't decided what to do here.
      }
    case Persisted(id, key) =>
      pendingAcks.get((id, key, persistence)) match {
        case Some(Ack(id, _, _, origin, _, _)) =>
          origin ! OperationAck(id)
          val t = (id, key, persistence)
          pendingAcks -= t
        case None => // TODO: Log something
      }
    case CheckAckFailed(id, key, target) =>
      pendingAcks.get((id, key,target)) match {
        case Some(Ack(_,_,_,origin,_,_)) =>
          target ! OperationFailed(id)
          val t = (id, key, origin)
          pendingAcks -= t
        case None => //
      }
  }

  private def checkForFailureEventually(id: Long, key: String, target: ActorRef) = {
    context.system.scheduler.scheduleOnce(1 second, self, CheckAckFailed(id, key, target))
  }

  private def handleKeyStoreOperations(): Receive = {
    case GetValue(id, k, replyTo) => sender() ! GetResult(id, k, cache.get(k), replyTo)
    case Update(id,k,v) =>
      updateKeyValueStore(k,Some(v))
      replicateNewValue(id, k, Some(v), self)
      persistNewValue(id, k, Some(v), sender())
    case Remove(id, key) =>
      updateKeyValueStore(key, None)
      replicateNewValue(id, key, None, self)
      persistNewValue(id, key, None, sender())
  }
}
