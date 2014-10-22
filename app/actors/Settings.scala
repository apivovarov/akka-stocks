/*
* Copyright © 2014 Typesafe, Inc. All rights reserved.
*/

package actors

import akka.actor.{ Actor, ExtendedActorSystem, Extension, ExtensionKey }
import akka.util.Timeout
import scala.concurrent.duration.{ Duration, FiniteDuration, MILLISECONDS => Millis }

object Settings extends ExtensionKey[Settings]

class Settings(system: ExtendedActorSystem) extends Extension {

    private val config = system.settings.config

    implicit val askTimeout: Timeout =
        Duration(config.getDuration("ask-timeout", Millis), Millis)

    val sentimentUrl = config.getString("sentiment.url")

    val defaultTweetUrl = config.getString("tweet.url")


}

trait SettingsActor {
  this: Actor =>

  val settings: Settings =
    Settings(context.system)
}
