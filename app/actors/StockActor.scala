package actors

import akka.actor._
import utils.FakeStockQuote
import scala.collection.immutable.{HashSet, Queue}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import actors.StockManagerActor._
import actors.StockActor.{AddWatcherAfterRecover, Snap, FetchLatest}
import scala.util.Random
import akka.persistence.PersistentActor

import akka.pattern.ask
import akka.util.Timeout
import actors.StockManagerActor.UnwatchStock
import actors.StockActor.AddWatcherAfterRecover
import actors.StockManagerActor.WatchStock
import actors.StockManagerActor.StockUpdate
import akka.actor.ActorIdentity
import akka.actor.Identify
import actors.StockManagerActor.StockHistory

/**
 * There is one StockActor per stock symbol.  The StockActor maintains a list of users watching the stock and the stock
 * values.  Each StockActor updates a rolling dataset of randomly generated stock values.
 */

class StockActor(symbol: String) extends PersistentActor with ActorLogging {

    implicit val identifyAskTimeout: Timeout = Duration(10, SECONDS)

    def persistenceId: String = {
        return "symbol_" + symbol
    }

    protected[this] var watchers: HashSet[ActorRef] = HashSet.empty[ActorRef]

    val rand = Random

    // A random data set which uses stockQuote.newPrice to get each data point
    var stockHistory: Queue[Double] = {
        lazy val initialPrices: Stream[Double] = (rand.nextDouble * 800) #:: initialPrices.map(previous => FakeStockQuote.newPrice(previous))
        initialPrices.take(50).to[Queue]
    }

    // Fetch the latest stock value every 75ms
    val stockTick = context.system.scheduler.schedule(Duration.Zero, 75.millis, self, FetchLatest)

    val snapshotTick = context.system.scheduler.schedule(Duration.Zero, 75.millis, self, Snap)

    override def receiveCommand: Receive = {
        case FetchLatest =>
            // add a new stock price to the history and drop the oldest
            val newPrice = FakeStockQuote.newPrice(stockHistory.last.doubleValue())
            stockHistory = stockHistory.drop(1) :+ newPrice
            log.info(s"## Broadcasting stock update")
            // notify watchers
            watchers.foreach(_ ! StockUpdate(symbol, newPrice))
        case WatchStock(_) =>
            log.info(s"## adding watch stock for: $sender")
            // send the stock history to the user
            sender ! StockHistory(symbol, stockHistory.toList)
            // add the watcher to the list
            watchers = watchers + sender
        case UnwatchStock(_) =>
            watchers = watchers - sender
            if (watchers.size == 0) {
                stockTick.cancel()
                context.stop(self)
            }
        case AddWatcherAfterRecover(watcher) => watchers = watchers + watcher

        case Snap => //todo take a snapshot
    }


    override def receiveRecover: Receive = {
        case _ =>
    }

    private def addWatcherIfAlive(watcher: ActorRef) {
        (watcher ? Identify(watcher.path.name)).mapTo[ActorIdentity].map {
            actorIdentity =>
                if (actorIdentity.getRef != null)
                    self ! AddWatcherAfterRecover(watcher)
        }.recover {
            case failure =>
                self ! UnwatchStock(Option(symbol))
        }
    }
}

object StockActor {

    def props(symbol: String): Props =
        Props(new StockActor(symbol))

    case object FetchLatest

    case class EventStockPriceUpdated(price: Double)

    case class EventWatcherAdded(watcher: ActorRef)

    case class EventWatcherRemover(watcher: ActorRef)

    case class TakeSnapshot(stockHistory: Queue[Double], watchers: HashSet[ActorRef])

    case class AddWatcherAfterRecover(watcher: ActorRef)

    case object Snap


}