/*
* Copyright © 2014 Typesafe, Inc. All rights reserved.
*/

package actors

import akka.actor.{ Actor, ExtendedActorSystem, Extension, ExtensionKey }

object Settings extends ExtensionKey[Settings]

class Settings(system: ExtendedActorSystem) extends Extension {

}

trait SettingsActor {
  this: Actor =>

  val settings: Settings =
    Settings(context.system)
}
