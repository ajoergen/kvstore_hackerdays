package controllers

import play.api.mvc._
import actors.{SpellcheckingWebSocketActor, SegmentationWebSocketActor}
import play.api.Play.current
import play.api.libs.json.JsValue

object Application extends Controller {

  def index = Action { implicit request =>
    Ok(views.html.index())
  }

  def segment = WebSocket.acceptWithActor[String, String] { request => out =>
    SegmentationWebSocketActor.props(out)
  }

  def spellcheck = WebSocket.acceptWithActor[String,String] {request=> out =>
    SpellcheckingWebSocketActor.props(out)
  }
}