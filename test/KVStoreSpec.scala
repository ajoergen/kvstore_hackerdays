import actors.{Persistence, Replica, KVStore}
import actors.KVStore._
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestKit, TestProbe}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.test.WithApplication


/**
 * Unit tests for sending messages for a SegmentationWebSocketActor.
 *
 * Created by anjj on 6/8/14.
 */
class KVStoreSpec extends Specification {

  class Actors extends TestKit(ActorSystem("ReplicaSpec")) with Scope
  "When given input thisisatest a SegmentationWebSocketActor" should {
    "return a message containing this is a test" in new Actors {
      new WithApplication {
        val probe = TestProbe()

        val myActor = system.actorOf(KVStore.props(1))
        probe.send(myActor, Join)
        probe.expectMsg(Replicas(Set(probe.ref)))
        probe.expectMsg(Joined)

        probe.send(myActor, FindReplica("test"))
        probe.expectMsg(probe.ref)
      }
    }
  }

  "When given two replicant actors, values " should {
    "be assigned different targets" in new Actors {
      new WithApplication {
        val probe = TestProbe()
        val kvStore = system.actorOf(KVStore.props(1))
        val repOne = system.actorOf(Replica.props(kvStore, Persistence.props(false)))
        val repTwo = system.actorOf(Replica.props(kvStore, Persistence.props(false)))

        Thread.sleep(200)
        probe.send(kvStore, Put("element1", 1231L))
        probe.send(kvStore, Put("element2", 1232L))
        probe.send(kvStore, Put("element3", 1233L))
        probe.send(kvStore, Put("element4", 1234L))
        probe.send(kvStore, Put("element5", 1235L))
        probe.send(kvStore, Put("element6", 1236L))
        probe.send(kvStore, Put("element7", 1237L))
        probe.send(kvStore, Put("element8", 1238L))
        Thread.sleep(200)
        probe.send(kvStore, Get("element"))

        var refSet = Set.empty[ActorRef]
        val replicants = Set(repOne, repTwo)

        probe.send(kvStore, FindReplica("element1"))
        (1 to 8).foreach { x =>
          probe.send(kvStore, FindReplica("element" + x))
        }
        probe.receiveN(8).foreach {
          case Some(ref) => refSet += ref.asInstanceOf[ActorRef]
          case _ => //
        }
        awaitAssert(refSet.equals(replicants))
      }
    }
  }
}