package controllers

import play.api.mvc.{WebSocket, Action, Controller}
import actors.UserActor
import play.api.libs.json.JsValue

import play.api.Play.current

object AkkaStocks extends Controller {

    def index = Action {
        Ok(views.html.index())
    }

    def ws = WebSocket.acceptWithActor[JsValue, JsValue] { implicit request => out =>
        UserActor.props(out)
    }

}
