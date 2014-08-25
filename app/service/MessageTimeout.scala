package service

import java.util.logging.Logger
import javax.print.CancelablePrintJob

import akka.actor.Actor.Receive
import akka.actor.{ActorLogging, Cancellable, Actor, ActorRef}
import sun.applet.resources.MsgAppletViewer
import scala.language.postfixOps
import scala.concurrent.duration._

/**
 * Created by bram on 8/25/14.
 */
trait MessageTimeout extends Actor with ActorLogging {
  import context.dispatcher

  case class Ack(id : Int)
  case class Msg(id: Int, msg : Any)
  var id = 0
  var resendMap : Map[Int, Cancellable] = Map.empty()

  def send(target : ActorRef, msg : Any): Unit = {
    target ! Msg(id, msg)
    id += 1
    var resendCount = 0
    val schedule = context.system.scheduler.schedule(100 milliseconds, 100 milliseconds, new Runnable {
      override def run(): Unit = {
        if(resendCount < 5){
          target ! (id, msg)
        } else {
          throw new RuntimeException("msg error")
        }
        resendCount += 1
      }
    })
    resendMap += id -> schedule
  }

  override def receive: Receive = {
    case Ack(id) => cancelSchedule(id)
    case Msg(id, msg) => {
      sender() ! Ack(id)
      myReceiver(msg)
    }
  }

  def myReceiver : Receive

  def cancelSchedule(id: Int): Unit = {
    resendMap.get(id) match {
      case None => log.error("scedule object for " + id + " could not be found")
      case Some(x) => {
        x.cancel()
        resendMap.drop(id)
      }
  }
}
