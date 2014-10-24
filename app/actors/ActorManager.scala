package actors

import akka.actor.{ExtendedActorSystem, Extension, ExtensionKey}
import scala.concurrent.duration.{MILLISECONDS => Millis}
import backend.SentimentActor

object ActorManager extends ExtensionKey[ActorManager]

class ActorManager(system: ExtendedActorSystem) extends Extension {

    val stockManagerActor = system.actorOf(StockManagerActor.props)

    val sentimentActor = system.actorOf(SentimentActor.props)

}
