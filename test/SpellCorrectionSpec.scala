import actors.SpellcheckingWebSocketActor
import akka.actor.ActorSystem
import akka.testkit.{TestProbe, TestKit}
import org.scalatest.time.Seconds
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.test.WithApplication
import scala.concurrent.duration.FiniteDuration

/**
 * Created by anjj on 6/8/14.
 */
class SpellCorrectionSpec extends Specification {

  class Actors extends TestKit(ActorSystem("SpellCorrectionSpec")) with Scope

  "When given input \"thew\" the SpellcheckingActor" should {
    "respond with message containing \"the\"" in new Actors {
      new WithApplication {
        val probe = TestProbe()


        val spellchecker = system.actorOf(SpellcheckingWebSocketActor.props(probe.ref))

        spellchecker ! "thew"


        val result = probe.expectMsg("the")
      }
    }
  }

  "When given input \"speling\" the SpellcheckingActor" should {
    "respond with a message containing \"spelling\"" in new Actors {
      new WithApplication {
        val probe = TestProbe()


        val spellchecker = system.actorOf(SpellcheckingWebSocketActor.props(probe.ref))

        spellchecker ! "speling"


        val result = probe.expectMsg("spelling")
      }
    }
  }

  "When given input \"vokabulary\" the SpellcheckingActor" should {
    "respond with a message containing \"vocabulary\"" in new Actors {
      new WithApplication {
        val probe = TestProbe()


        val spellchecker = system.actorOf(SpellcheckingWebSocketActor.props(probe.ref))

        spellchecker ! "vokabulary"


        val result = probe.expectMsg("vocabulary")
      }
    }
  }
}
