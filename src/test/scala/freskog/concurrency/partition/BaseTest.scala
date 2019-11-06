package freskog.concurrency.partition

import java.util.concurrent.TimeUnit.MILLISECONDS

import zio._
import zio.console.{ Console }
import zio.test._
import zio.test.Assertion.{ equalTo }
import zio.test.TestAspect.{ timeout }
import zio.stm.{ TQueue }
import zio.duration._
import zio.clock.Clock

import freskog.concurrency.partition._
import freskog.concurrency.partition.Partition._
import freskog.concurrency.partition.Common.{ PartEnv }

object STMSpec
    extends ZIOBaseSpec(
      suite("STM spec")(
        suite("producer")(
          testM("return true when publishing to an empty TQueue") {
            (for {
              q         <- TQueue.make[Int](1)
              published <- publish(q, 1)
            } yield assert(published, equalTo(true))).commit
          },
          testM("return false when publishing to a full TQueue") {
            (for {
              q         <- TQueue.make[Int](0)
              published <- publish(q, 1)
            } yield assert(published, equalTo(false))).commit
          }
        ),
        suite("consumer")(
          testM("return false when publishing to a full TQueue") {
            (for {
              q         <- TQueue.make[Int](0)
              published <- publish(q, 1)
            } yield assert(published, equalTo(false))).commit
          } @@ timeout(1.nanos),
          testM("always successfully process a value on the queue") {
            val config =
              Config(
                userTTL = Duration(100, MILLISECONDS),
                idleTTL = Duration(100, MILLISECONDS),
                maxPending = 1
              )

            def partEnv(config: Config): ZIO[Clock with Console, Nothing, PartEnv] =
              ZIO.environment[Clock with Console].map(Partition.buildEnv(config, _))

            for {
              env     <- partEnv(config)
              queue   <- TQueue.make[String](1).commit
              promise <- Promise.make[Nothing, String]
              latch   = promise.succeed(_: String).unit
              _       <- startConsumer("p1", queue, UIO.unit, latch).provide(env)
              _       <- queue.offer("published").commit
              result  <- promise.await
            } yield assert(result, equalTo("published"))
          } @@ timeout(150.millis)
        )
      )
    )
