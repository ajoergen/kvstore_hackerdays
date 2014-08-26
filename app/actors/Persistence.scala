package actors


import actors.Replica.Persist
import akka.actor.{ActorLogging, Actor, Props}

import scala.util.Random

object Persistence {
  case class Persisted(id: Long, key: String)

  class PersistenceException extends Exception("Persistence failure")

  def props(flaky: Boolean): Props = Props(classOf[Persistence], flaky)
}

class Persistence(flaky: Boolean) extends Actor with ActorLogging {
  import actors.Persistence._

  def receive = {
    case Persist(id, key, valueOption) =>
      if (!flaky || Random.nextInt(100) > 90)
        sender ! Persisted(id: Long, key: String)
      else
        log.warning("persistence failed for [id:%d,key:%s]".format(id, key))
  }

}
