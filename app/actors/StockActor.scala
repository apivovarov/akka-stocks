package actors

import akka.actor.{Props, ActorRef}
import utils.FakeStockQuote
import scala.collection.immutable.{HashSet, Queue}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import actors.StockActor._
import scala.util.Random
import akka.persistence.PersistentActor
import actors.StockActor.EventWatchStock
import actors.StockManagerActor.UnwatchStock
import akka.persistence.SnapshotOffer
import actors.StockManagerActor.WatchStock
import actors.StockActor.EventStockUpdate
import actors.StockManagerActor.StockUpdate
import akka.persistence.serialization.Snapshot
import actors.StockManagerActor.StockHistory

/**
 * There is one StockActor per stock symbol.  The StockActor maintains a list of users watching the stock and the stock
 * values.  Each StockActor updates a rolling dataset of randomly generated stock values.
 */

class StockActor(symbol: String) extends PersistentActor {

    override val persistenceId: String = s"stock-$symbol"

    protected[this] var watchers: HashSet[ActorRef] = HashSet.empty[ActorRef]

    val rand = Random

    // A random data set which uses stockQuote.newPrice to get each data point
    var stockHistory: Queue[Double] = {
        lazy val initialPrices: Stream[Double] = (rand.nextDouble * 800) #:: initialPrices.map(previous => FakeStockQuote.newPrice(previous))
        initialPrices.take(50).to[Queue]
    }

    // Fetch the latest stock value every 75ms
    val stockTick = context.system.scheduler.schedule(Duration.Zero, 75.millis, self, FetchLatest)

    override def receiveCommand: Receive = {
        case FetchLatest =>
            // add a new stock price to the history and drop the oldest
            val newPrice = FakeStockQuote.newPrice(stockHistory.last.doubleValue())
            persist(EventStockUpdate(newPrice)) { eventStockUpdate =>
                onStockUpdate(eventStockUpdate.price)
                // notify watchers
                watchers.foreach(_ ! StockUpdate(symbol, newPrice))
            }
        case WatchStock(_) =>
            persist(EventWatchStock(sender)) { eventWatchStock =>
                // send the stock history to the user
                eventWatchStock.watcher ! StockHistory(symbol, stockHistory.toList)
                onWatchStock(eventWatchStock.watcher)
            }
        case UnwatchStock(_) =>
            persist(EventUnwatchStock(sender)) { eventUnwatchStock =>
                onUnwatchStock(eventUnwatchStock.watcher)
                if (watchers.size == 0) {
                    stockTick.cancel()
                    context.stop(self)
                }
            }
    }

    override def receiveRecover: Receive = {
        case eventStockUpdate: EventStockUpdate => onStockUpdate(eventStockUpdate.price)
        case eventWatchStock: EventWatchStock => onWatchStock(eventWatchStock.watcher)
        case eventUnwatchStock: EventUnwatchStock => onUnwatchStock(eventUnwatchStock.watcher)
        case SnapshotOffer(_, snapshot: Snapshot) => //TODO - handle snapshot
    }

    def onStockUpdate(newPrice: Double) = {
        stockHistory = stockHistory.drop(1) :+ newPrice
    }
    def onWatchStock(watcher: ActorRef) = {
        // add the watcher to the list
        watchers = watchers + watcher
    }
    def onUnwatchStock(watcher: ActorRef) = {
        watchers = watchers - watcher
    }

}

object StockActor {

    def props(symbol: String): Props =
        Props(new StockActor(symbol))

    case object FetchLatest

    case class EventStockUpdate(price: Double)

    case class EventWatchStock(watcher: ActorRef)

    case class EventUnwatchStock(watcher: ActorRef)


}