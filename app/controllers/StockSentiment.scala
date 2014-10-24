package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import play.api.mvc._
import play.api.libs.ws._
import scala.concurrent.Future
import play.api.libs.json._
import play.api.Play
import actors.{ActorManager, Settings}
import play.libs.Akka
import model.Tweet
import akka.actor.ActorRef
import backend.SentimentActor

import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration.{Duration, SECONDS}
import scala.util.Failure
import play.api.libs.json.JsString
import play.api.mvc.Result
import scala.util.Success


object StockSentiment extends Controller {


    def getTextSentiment(text: String): Future[WSResponse] =
        WS.url(Settings(Akka.system).sentimentUrl) post Map("text" -> Seq(text))

    def getAverageSentiment(responses: Seq[WSResponse], label: String): Double = responses.map { response =>
        (response.json \\ label).head.as[Double]
    }.sum / responses.length.max(1) // avoid division by zero

    def loadSentimentFromTweets(json: JsValue): Seq[Future[WSResponse]] =
        (json \ "statuses").as[Seq[Tweet]] map (tweet => getTextSentiment(tweet.text))

    def getTweets(symbol: String): Future[WSResponse] = {
        val symbolTweetUrl = Settings(Akka.system).defaultTweetUrl.format(symbol)
        WS.url(symbolTweetUrl).get.withFilter { response =>
            response.status == OK
        }
    }

    def sentimentJson(sentiments: Seq[WSResponse]) = {
        val neg = getAverageSentiment(sentiments, "neg")
        val neutral = getAverageSentiment(sentiments, "neutral")
        val pos = getAverageSentiment(sentiments, "pos")

        val response = Json.obj(
            "probability" -> Json.obj(
                "neg" -> neg,
                "neutral" -> neutral,
                "pos" -> pos
            )
        )

        val classification =
            if (neutral > 0.5)
                "neutral"
            else if (neg > pos)
                "neg"
            else
                "pos"

        response + ("label" -> JsString(classification))
    }

    val sentimentActor: ActorRef = ActorManager(Akka.system()).sentimentActor
    implicit val sentimentAskTimeout: Timeout = Duration(10, SECONDS)

    def get(symbol: String): Action[AnyContent] = Action.async {

        (sentimentActor ? SentimentActor.GetSentiment(symbol)).mapTo[JsObject].map {
            sentimentJson => {
                println(s"returning sentimentJson: $sentimentJson")
                Ok(sentimentJson)
            }
        }

        /* need to add proper error handling
            futureStockSentiments.recover {
                case nsee: NoSuchElementException =>
                    InternalServerError(Json.obj("error" -> JsString("Could not fetch the tweets")))
            }
        */
    }
}
