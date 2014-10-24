package backend

import akka.actor.ActorSystem
import actors.{StockManagerActor, Settings}
import backend.journal.SharedJournalSetter

/**
 * Main class for starting cluster nodes.
 */
object MainClusterManager extends BaseApp {

    override protected def initialize(system: ActorSystem, settings: Settings): Unit =
    {
        system.actorOf(SharedJournalSetter.props, "shared-journal-setter")

        system.actorOf(StockManagerActor.props, "stockManager")
    }
}