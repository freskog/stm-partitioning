package freskog.concurrency.partition


import scalaz.zio.{Ref, Runtime}
import scalaz.zio.clock.Clock
import scalaz.zio.console.Console
import scalaz.zio.internal.{Platform, PlatformLive}
import scalaz.zio.scheduler.Scheduler
import scalaz.zio.testkit.{TestClock, TestConsole, TestScheduler}


case class TestRuntime(clockR:Ref[TestClock.Data], consoleR:Ref[TestConsole.Data], schedR:Ref[TestClock.Data]) extends Runtime[Clock with Console] { self =>
  type Environment = Clock with Console

  val Platform: Platform       = PlatformLive.Default
  val Environment: Environment =
    new Clock with Console {
      override val clock: Clock.Service[Any] = TestClock(clockR)
      override val console: Console.Service[Any] = TestConsole(consoleR)
      override val scheduler: Scheduler.Service[Any] = TestScheduler(schedR, self)
    }
}
