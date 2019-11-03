package freskog.concurrency.partition

import zio._
import zio.clock._
import zio.console._
import zio.duration.Duration
import zio.stm._
import zio.blocking.Blocking
import zio.random.Random

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
