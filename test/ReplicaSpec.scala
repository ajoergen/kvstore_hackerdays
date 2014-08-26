import actors.KVStore.Join
import actors.Replica.{GetResult, Get, Update}
import actors.{Persistence, Replica}
import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.test.WithApplication


/**
 * Unit tests for sending messages for a SegmentationWebSocketActor.
 *
 * Created by anjj on 6/8/14.
 */
class ReplicaSpec extends Specification {

  class Actors extends TestKit(ActorSystem("ReplicaSpec")) with Scope

  "When a Replica is started a Join message" should {
    "be send to the parent actor" in new Actors {
      new WithApplication {
        val probe = TestProbe()

        val replica = system.actorOf(Replica.props(probe.ref, Persistence.props(false)))
        probe.expectMsg(Join)
      }
    }
  }

  "When a Replica receives Get in an empty kvstore it" should {
    "respond with None" in new Actors {
      new WithApplication() {
        val probe = TestProbe()

        val replica = system.actorOf(Replica.props(probe.ref, Persistence.props(false)))
        probe.expectMsg(Join)

        probe.send(replica, Get(1, "test"))
        probe.expectMsg(GetResult(1, "test", None))
      }
    }
  }

  "When a Replica receives Update you" should {
    "be able to get the value subsequently" in new Actors {
      new WithApplication() {
        val probe = TestProbe()

        val replica = system.actorOf(Replica.props(probe.ref, Persistence.props(false)))
        probe.expectMsg(Join)

        probe.send(replica, Update(1, "test", 42L))
        probe.send(replica, Get(2,  "test"))
        probe.expectMsg(GetResult(2, "test", Some(42L)))
      }
    }
  }
}

