package freskog.concurrency.partition

import zio._
import zio.console._
import zio.test._
import zio.test.Assertion._
import zio.test.environment._
import zio.stm._
import zio.duration.Duration
import zio.clock._

import freskog.concurrency.partition.Partition._

object STMSpec
    extends DefaultRunnableSpec(
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
        //   testM ("newone") {
        //     for {
        //       env     <- partEnv(config)
        //       queue   <- TQueue.make[String](1).commit
        //       promise <- Promise.make[Nothing,String]
        //       _       <- startConsumer("p1", queue, UIO.unit, promise.succeed(_:String).unit).provide(env)
        //       _       <- queue.offer("published").commit
        //       result  <- promise.await.timeoutFail("not published")(Duration(150,MILLISECONDS)).fold(identity,identity)
        //     } yield assert(result, equalTo("published"))
        //   }
      )
    )
