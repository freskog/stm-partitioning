package freskog.concurrency.app


import java.util.concurrent.TimeUnit

import scalaz.zio.blocking.Blocking
import scalaz.zio.duration.Duration
import scalaz.zio.random.Random
import scalaz.zio.system.System
import scalaz.zio._
import scalaz.zio.clock._
import scalaz.zio.console._

import scala.concurrent.duration._
import freskog.concurrency.partition._

object PartitioningDemo extends App {

  val config:Config = Config(userTTL = Duration(3, SECONDS), idleTTL = Duration(2, SECONDS), maxPending = 1)

  def brokenUserFunction(startTs:Long, counts:Ref[Map[Int,Int]])(n:Int): ZIO[Console with Clock, Nothing, Unit] =
    ZIO.descriptorWith( desc =>
      for {
        now <- currentTime (MILLISECONDS) <* sleep(Duration(1000, MILLISECONDS))
        aft <- currentTime (MILLISECONDS)
          m <- counts.get
        msg = s"Consumed successfully at ${now - startTs}ms, done at ${aft - startTs}ms - Fiber: ${desc.id}, n = $n (call #${m(n)})"
        _   <-  putStrLn(msg)
      } yield ()
    )

  val workItems: List[Int] = List.range(0,11) ::: List.range(0,11) ::: List.range(0, 11) ::: List(30)

  val time: ZIO[Clock, Nothing, Long] = clock.currentTime(TimeUnit.MILLISECONDS)

  def printIfPublished(startTs:Long, counts:Ref[Map[Int,Int]])(n:Int)(wasPublished:Boolean) =
    ZIO.descriptorWith(desc =>
      for {
        now <- time
          _ <- if(wasPublished)
                counts.update(m => m.updated(n, m.getOrElse(n, 0) + 1)) >>= (m =>
                  putStrLn(s"Published successfully at ${now - startTs}ms, - Fiber: ${desc.id}, n = $n (call #${m(n)})"))
               else
                  UIO.unit
      } yield wasPublished
    )

  val program: ZIO[Environment with Partition, Nothing, Int] =
    for {
      startAt <- time
      counter <- Ref.make(Map.empty[Int,Int])
          env <- ZIO.environment[Console with Clock]
      process <- partition[Int](config, _.toString, brokenUserFunction(startAt,counter)(_).provide(env))
      results <- ZIO.foreach(workItems)(a => process(a) >>= printIfPublished(startAt, counter)(a))
            _ <- console.putStrLn(s"Published ${results.count(identity)} out of ${results.length}")
            _ <- ZIO.sleep(Duration.fromScala(10.seconds))
    } yield 0

  override def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    program.provideSome[Environment]( env =>
      new Clock with Console with System with Random with Blocking with Partition.Live {
        override val  blocking:  Blocking.Service[Any] = env.blocking
        override val     clock:     Clock.Service[Any] = env.clock
        override val   console:   Console.Service[Any] = env.console
        override val    random:    Random.Service[Any] = env.random
        override val    system:    System.Service[Any] = env.system
      })

}
