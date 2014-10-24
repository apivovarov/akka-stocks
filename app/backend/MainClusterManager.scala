package backend

import akka.actor.ActorSystem
import actors.{StockManagerActor, Settings}

/**
 * Main class for starting cluster nodes.
 */
object MainClusterManager extends BaseApp {

    override protected def initialize(system: ActorSystem, settings: Settings): Unit =
    {
        val stockManagerActor = system.actorOf(StockManagerActor.props, "stockManager")
    }
}