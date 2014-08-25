import actors.KVStore
import actors.KVStore.Join
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
class KVStoreSpec extends Specification {

  class Actors extends TestKit(ActorSystem("ReplicaSpec")) with Scope

  "When given input thisisatest a SegmentationWebSocketActor" should {
    "return a message containing this is a test" in new Actors {
      new WithApplication {
        val probe = TestProbe()

        val myActor = system.actorOf(KVStore.props(1))
        probe.expectMsg(Join)
      }
    }
  }
}

