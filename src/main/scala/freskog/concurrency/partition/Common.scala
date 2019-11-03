package freskog.concurrency.partition

import zio.clock.{ Clock }
import zio.console.{ Console }
import zio.duration.Duration
import zio.stm.{ TQueue, TRef }

trait Conf {
  def userTTL: Duration
  def idleTTL: Duration
  def maxPending: Int
}

case class Config(userTTL: Duration, idleTTL: Duration, maxPending: Int)

package object Common {

  type PartEnv   = Clock with Console with Conf
  type Queues[A] = TRef[Map[PartId, TQueue[A]]]

}
