package actors

import akka.actor.{ExtendedActorSystem, Extension, ExtensionKey}
import scala.concurrent.duration.{MILLISECONDS => Millis}

import backend.SentimentActor
import akka.routing.FromConfig

object ActorManager extends ExtensionKey[ActorManager]

class ActorManager(system: ExtendedActorSystem) extends Extension {
    val stockManagerProxy = system.actorOf(StockManagerProxy.props)
    val sentimentActor = system.actorOf(FromConfig.props(SentimentActor.props),"sentimentRouter")
}
