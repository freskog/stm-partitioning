package freskog.concurrency.partition


import zio.{Ref, Runtime}
import zio.clock.Clock
import zio.console.Console
import zio.internal.{Platform, PlatformLive}
import zio.scheduler.Scheduler
import zio.testkit.{TestClock, TestConsole, TestScheduler}


case class TestRuntime(clockR:Ref[TestClock.Data], consoleR:Ref[TestConsole.Data], schedR:Ref[TestClock.Data]) extends Runtime[Clock with Console] { self =>
  type Environment = Clock with Console

  val Platform: Platform       = PlatformLive.Default
  val Environment: Environment =
    new Clock with Console {
      override val clock: Clock.Service[Any] = TestClock(clockR)
      override val console: Console.Service[Any] = TestConsole(consoleR)
    }
}
