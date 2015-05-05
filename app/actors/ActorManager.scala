package actors

import akka.actor._

object ActorManager extends ExtensionKey[ActorManager]

class ActorManager(system: ExtendedActorSystem) extends Extension {

  val stockManagerActor: ActorRef = system.actorOf(Props(classOf[StockManagerActor]))
}

trait ActorManagerActor { this: Actor =>
  val actorManager: ActorManager = ActorManager(context.system)
}
