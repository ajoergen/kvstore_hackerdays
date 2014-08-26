package actors


import actors.Replica.Persist
import akka.actor.{Actor, Props}

import scala.util.Random

object Persistence {
  case class Persisted(id: Long, key: String)

  class PersistenceException extends Exception("Persistence failure")

  def props(flaky: Boolean): Props = Props(classOf[Persistence], flaky)
}

class Persistence(flaky: Boolean) extends Actor {
  import actors.Persistence._

  def receive = {
    case Persist(id, key, valueOption) =>
      if (!flaky || Random.nextBoolean()) sender ! Persisted(id: Long, key: String)
      else throw new PersistenceException
  }

}
