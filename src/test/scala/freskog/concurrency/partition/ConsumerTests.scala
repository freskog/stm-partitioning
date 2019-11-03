package freskog.concurrency.partition

import java.util.concurrent.TimeUnit.MILLISECONDS

import freskog.concurrency.partition.Partition.startConsumer
import zio.duration.Duration
import zio.{ Promise, UIO }
import zio.stm.TQueue

class ConsumerTests extends BaseTests {

  val config =
    Config(
      userTTL = Duration(100, MILLISECONDS),
      idleTTL = Duration(100, MILLISECONDS),
      maxPending = 1
    )

  behavior of "a consumer"

  it should "always successfully process a value on the queue" in {
    runReal(
      for {
        env     <- partEnv(config)
        queue   <- TQueue.make[String](1).commit
        promise <- Promise.make[Nothing, String]
        _       <- startConsumer("p1", queue, UIO.unit, promise.succeed(_: String).unit).provide(env)
        _       <- queue.offer("published").commit
        result  <- promise.await.timeoutFail("not published")(Duration(150, MILLISECONDS)).fold(identity, identity)
      } yield assert(result == "published")
    )
  }

  it should "a defect in client code doesn't break the consumer" in {
    runReal(
      for {
        env     <- partEnv(config)
        queue   <- TQueue.make[Boolean](1).commit
        promise <- Promise.make[Nothing, String]
        _ <- startConsumer(
              "p1",
              queue,
              UIO.unit,
              (b: Boolean) => if (b) throw new IllegalArgumentException("BOOM!") else promise.succeed("done!").unit
            ).provide(env)
        _      <- queue.offerAll(List(true, false)).commit
        result <- promise.await.timeoutFail("not published")(Duration(150, MILLISECONDS)).fold(identity, identity)
      } yield assert(result == "done!")
    )
  }
}
