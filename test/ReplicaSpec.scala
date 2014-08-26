import actors.KVStore.{Replicas, Join}
import actors.Replica._
import actors.{Persistence, Replica}
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestKit, TestProbe}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.test.WithApplication
import scala.language.postfixOps


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

        probe.send(replica, GetValue(1, "test", probe.ref))
        probe.expectMsg(GetResult(1, "test", None, probe.ref))
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
        probe.send(replica, GetValue(2,  "test", probe.ref))
        probe.expectMsg(GetResult(2, "test", Some(42L), probe.ref))
      }
    }
  }

  "When a Replica receives an update the value" should {
    "be replicated" in new Actors {
      new WithApplication() {
        val kvStore = TestProbe()

        val replica = system.actorOf(Replica.props(kvStore.ref, Persistence.props(false)))
        kvStore.expectMsg(Join)
        val replicaProbe = TestProbe()
        val replicas = Set(replica, replicaProbe.ref)
        kvStore.send(replica, Replicas(replicas))

        kvStore.send(replica, Update(1, "test", 42L))
        kvStore.expectMsg(OperationAck(1))
        (0 until 5).foreach { i =>
          replicaProbe.expectMsg(Replicate(1, "test", Some(42L)))
        }
        replicaProbe.send(replica, Replicated(1, "test", Some(42L)))
      }
    }
  }
}

