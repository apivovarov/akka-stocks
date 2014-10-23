package actors

import akka.actor.{Props, ActorRef, Actor}
import utils.FakeStockQuote
import scala.collection.immutable.{HashSet, Queue}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import actors.StockManagerActor._
import actors.StockActor.FetchLatest
import scala.util.Random

/**
 * There is one StockActor per stock symbol.  The StockActor maintains a list of users watching the stock and the stock
 * values.  Each StockActor updates a rolling dataset of randomly generated stock values.
 */

class StockActor(symbol: String) extends Actor {

    protected[this] var watchers: HashSet[ActorRef] = HashSet.empty[ActorRef]

    val rand = Random

    // A random data set which uses stockQuote.newPrice to get each data point
    var stockHistory: Queue[Double] = {
        lazy val initialPrices: Stream[Double] = (rand.nextDouble * 800) #:: initialPrices.map(previous => FakeStockQuote.newPrice(previous))
        initialPrices.take(50).to[Queue]
    }

    // Fetch the latest stock value every 75ms
    val stockTick = context.system.scheduler.schedule(Duration.Zero, 75.millis, self, FetchLatest)

    def receive = {
        case FetchLatest =>
            // add a new stock price to the history and drop the oldest
            val newPrice = FakeStockQuote.newPrice(stockHistory.last.doubleValue())
            stockHistory = stockHistory.drop(1) :+ newPrice
            // notify watchers
            watchers.foreach(_ ! StockUpdate(symbol, newPrice))
        case WatchStock(_) =>
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
    }
}

object StockActor {

    def props(symbol: String): Props =
        Props(new StockActor(symbol))

    case object FetchLatest


}