package freskog.concurrency.app

import java.util.concurrent.TimeUnit

import zio.blocking.Blocking
import zio.duration.Duration
import zio.random.Random
import zio.system.System
import zio._
import zio.clock._
import zio.console._

import scala.concurrent.duration._
import freskog.concurrency.partition._

object PartitioningDemo extends App {
  type Environment = Console with Clock with Blocking with Random with System

  val config: Config = Config(userTTL = Duration(3, SECONDS), idleTTL = Duration(2, SECONDS), maxPending = 3)

  def brokenUserFunction(startTs: Long, counts: Ref[Map[Int, Int]])(n: Int): ZIO[Console with Clock, Nothing, Unit] =
    ZIO.descriptorWith(
      desc =>
        for {
          now <- sleep(Duration(100 * n, MILLISECONDS)) *> currentTime(MILLISECONDS)
          m   <- counts.update(m => m.updated(n, m.getOrElse(n, 0) + 1))
          msg = s"Offset: ${now - startTs}ms Fiber: ${desc.id}, n = $n (call #${m(n)})"
          _   <- if (n == 0) throw new IllegalArgumentException(msg) else putStrLn(msg)
        } yield ()
    )

  val workItems: List[Int] = List.range(0, 11) ::: List.range(0, 11) ::: List.range(0, 11) ::: List(30)

  val program: ZIO[Environment with Partition, Nothing, Int] =
    for {
      now     <- clock.currentTime(TimeUnit.MILLISECONDS)
      counter <- Ref.make(Map.empty[Int, Int])
      env     <- ZIO.environment[Console with Clock]
      process <- partition[Int](config, _.toString, brokenUserFunction(now, counter)(_).provide(env))
      results <- ZIO.foreach(workItems)(process)
      _       <- console.putStrLn(s"Published ${results.count(identity)} out of ${results.length}")
      _       <- ZIO.sleep(Duration.fromScala(10.seconds))
    } yield 0

  override def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    program.provideSome[Environment](
      env =>
        new Clock with Console with System with Random with Blocking with Partition.Live {
          override val blocking: Blocking.Service[Any] = env.blocking
          override val clock: Clock.Service[Any]       = env.clock
          override val console: Console.Service[Any]   = env.console
          override val random: Random.Service[Any]     = env.random
          override val system: System.Service[Any]     = env.system
        }
    )

}
