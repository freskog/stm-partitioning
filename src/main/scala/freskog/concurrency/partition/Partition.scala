package freskog.concurrency.partition

import zio.{ UIO, ZIO }
import zio.clock.{ Clock }
import zio.console.{ putStrLn, Console }
import zio.duration.Duration
import zio.stm.{ STM, TQueue, TRef }

import freskog.concurrency.partition.Common._

trait Partition extends Serializable {
  val partition: Partition.Service[Any]
}

object Partition extends Serializable {

  trait Service[R] extends Serializable {
    def partition[A](config: Config, partIdOf: A => PartId, action: A => UIO[Unit]): ZIO[R, Nothing, A => UIO[Boolean]]
  }

  trait Live extends Clock.Live with Console.Live with Partition {
    env =>

    override val partition: Service[Any] =
      new Service[Any] {
        override def partition[A](
          config: Config,
          partIdOf: A => PartId,
          action: A => UIO[Unit]
        ): ZIO[Any, Nothing, A => UIO[Boolean]] =
          TRef
            .make(Map.empty[PartId, TQueue[A]])
            .commit
            .map(
              queues => producer(queues, partIdOf, action)(_).provide(buildEnv(config, env))
            )
      }
  }

  def buildEnv(conf: Config, env: Clock with Console): PartEnv =
    new Conf with Clock with Console {
      override def userTTL: Duration = conf.userTTL
      override def idleTTL: Duration = conf.idleTTL
      override def maxPending: Int   = conf.maxPending

      override val clock: Clock.Service[Any]     = env.clock
      override val console: Console.Service[Any] = env.console
    }

  val userTTL: ZIO[Conf, Nothing, Duration] =
    ZIO.access[Conf](_.userTTL)

  val idleTTL: ZIO[Conf, Nothing, Duration] =
    ZIO.access[Conf](_.idleTTL)

  val maxPending: ZIO[Conf, Nothing, Int] =
    ZIO.access[Conf](_.maxPending)

  def publish[A](queue: TQueue[A], a: A): STM[Nothing, Boolean] =
    queue.size.flatMap(size => if (size == queue.capacity) STM.succeed(false) else queue.offer(a) *> STM.succeed(true))

  def debug(cause: zio.Cause[String]): ZIO[Console, Nothing, Unit] =
    putStrLn(cause.prettyPrint)

  def takeNextMessageOrTimeout[A](id: PartId, queue: TQueue[A]): ZIO[Clock with Conf, String, A] =
    idleTTL flatMap queue.take.commit.timeoutFail(s"$id consumer expired")

  def safelyPerformAction[A](id: PartId, action: A => UIO[Unit])(a: A): ZIO[PartEnv, Nothing, Unit] =
    (userTTL flatMap (action(a).timeoutFail(s"$id action timed out")(_))).sandbox.catchAll(debug)

  def startConsumer[A](
    id: PartId,
    queue: TQueue[A],
    cleanup: UIO[Unit],
    action: A => UIO[Unit]
  ): ZIO[PartEnv, Nothing, Unit] =
    (takeNextMessageOrTimeout(id, queue) flatMap safelyPerformAction(id, action)).forever.option
      .ensuring(cleanup)
      .fork
      .unit

  def hasConsumer[A](queues: Queues[A], id: PartId): STM[Nothing, Boolean] =
    queues.get.map(_.contains(id))

  def removeConsumerFor[A](queues: Queues[A], id: PartId): UIO[Unit] =
    queues.update(_ - id).unit.commit

  def getWorkQueueFor[A](queues: Queues[A], id: PartId): STM[Nothing, TQueue[A]] =
    queues.get.map(_(id))

  def setWorkQueueFor[A](queues: Queues[A], id: PartId, queue: TQueue[A]): STM[Nothing, Unit] =
    queues.update(_.updated(id, queue)).unit

  def createConsumer[A](
    queues: Queues[A],
    id: PartId,
    maxPending: Int,
    action: A => UIO[Unit]
  ): STM[Nothing, ZIO[PartEnv, Nothing, Unit]] =
    for {
      queue <- TQueue.make[A](maxPending)
      _     <- setWorkQueueFor(queues, id, queue)
    } yield startConsumer(id, queue, removeConsumerFor(queues, id), action)

  def producer[A](queues: Queues[A], partIdOf: A => PartId, action: A => UIO[Unit])(
    a: A
  ): ZIO[PartEnv, Nothing, Boolean] =
    maxPending >>= { maxPending: Int =>
      STM.atomically {
        for {
          exists    <- hasConsumer(queues, partIdOf(a))
          id        = partIdOf(a)
          consumer  <- if (exists) STM.succeed(ZIO.unit) else createConsumer(queues, id, maxPending, action)
          queue     <- getWorkQueueFor(queues, partIdOf(a))
          published <- publish(queue, a)
        } yield ZIO.succeed(published) <* consumer
      }.flatten
    }

  object Live extends Live
}
