package actors


import actors.Replica.Entry
import akka.actor.{Props, Actor}

import scala.util.Random

object Persistence {
  case class Persist(id: Long, entry: Entry)
  case class Persisted(id: Long, entry: Entry)

  class PersistenceException extends Exception("Persistence failure")

  def props(flaky: Boolean): Props = Props(classOf[Persistence], flaky)
}

class Persistence(flaky: Boolean) extends Actor {
  import Persistence._

  def receive = {
    case Persist(id, entry) =>
      if (!flaky || Random.nextBoolean()) sender ! Persisted(id, entry)
      else throw new PersistenceException
  }

}
