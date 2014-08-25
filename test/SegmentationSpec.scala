import actors.SegmentationWebSocketActor
import akka.actor.ActorSystem
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{Matchers, FunSuiteLike, BeforeAndAfterAll}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.test.WithApplication
import service.ProbabilityDistribution


/**
 * Unit tests for sending messages for a SegmentationWebSocketActor.
 *
 * Created by anjj on 6/8/14.
 */
class SegmentationSpec extends Specification {

  class Actors extends TestKit(ActorSystem("SegmentationSpec")) with Scope

  "When given input thisisatest a SegmentationWebSocketActor" should {
    "return a message containing this is a test" in new Actors {
      new WithApplication {
        val probe = TestProbe()

        val myActor = system.actorOf(SegmentationWebSocketActor.props(probe.ref))
        myActor ! "thisisatest"

        probe.expectMsg("this is a test")
      }
    }
  }
}

