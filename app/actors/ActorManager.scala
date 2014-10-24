package actors

import akka.actor.{ExtendedActorSystem, Extension, ExtensionKey}
import scala.concurrent.duration.{MILLISECONDS => Millis}

import backend.SentimentActor
import akka.routing.FromConfig
import backend.journal.SharedJournalSetter

object ActorManager extends ExtensionKey[ActorManager]

class ActorManager(system: ExtendedActorSystem) extends Extension {

    val stockManagerProxy = system.actorOf(StockManagerProxy.props)

    val sentimentActor = system.actorOf(FromConfig.props(SentimentActor.props),"sentimentRouter")

    system.actorOf(SharedJournalSetter.props, "shared-journal-setter")
}
