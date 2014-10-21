package backend

import akka.actor.ActorSystem
import scala.io.StdIn
import scala.collection.breakOut

/**
 * Main class for starting cluster nodes.
 */
object MainClusterManager {

    def main(args: Array[String]): Unit = {

        val opts = MainUtils.argsToOpts(args.toList)
        MainUtils.applySystemProperties(opts)

        // Create an actor system with the name of application - this is the same name
        // that play uses for it's actor system.  The names need to be the same so they
        // can join together in a cluster.
        val system = ActorSystem("application")

        commandLoop(system)
    }

    def commandLoop(system: ActorSystem): Unit = {
        val line: String = StdIn.readLine()
        if (line.startsWith("s")) {
            system.shutdown()
        } else {
            commandLoop(system)
        }
    }
}

object MainUtils {

    val Opt = """(\S+)=(\S+)""".r

    def argsToOpts(args: Seq[String]): Map[String, String] =
        args.collect { case Opt(key, value) => key -> value }(breakOut)

    def applySystemProperties(options: Map[String, String]): Unit =
        for ((key, value) <- options if key startsWith "-D")
            System.setProperty(key substring 2, value)
}