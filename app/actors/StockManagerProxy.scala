package actors

import akka.actor._
import akka.cluster.Cluster
import akka.actor.RootActorPath
import akka.cluster.ClusterEvent.MemberUp
import scala.Some
import actors.StockManagerActor.{UnwatchStock, WatchStock}


class StockManagerProxy extends Actor with ActorLogging  with Stash {

    Cluster(context.system).subscribe(self, classOf[MemberUp])
    var optStockManager = Option.empty[ActorSelection]

    def watchForStockManager: Receive = {
        case MemberUp(member) if member.hasRole("backend") => {
            val path = RootActorPath(member.address) / "user" / "stockManager"
            val stockManager = context actorSelection path
            optStockManager = Some(stockManager)
            unstashAll()
            context.become(ready)
        }
        case _ => {
            log.info(s"waiting for member up")
            stash()
        }
    }

    override def receive: Receive =
        watchForStockManager

    def ready: Receive = {
        case watchStock:WatchStock => optStockManager match {
            case Some(stockManager) => stockManager forward watchStock
            case None => log.error("in ready state but stockManager was not set")
        }
        case unwatchStock:UnwatchStock => optStockManager match {
            case Some(stockManager) => stockManager forward unwatchStock
            case None => log.error("in ready state but stockManager was not set")
        }
    }
}

object StockManagerProxy {
    def props(): Props =
        Props(new StockManagerProxy())
}
