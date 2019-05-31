package freskog.concurrency.partition

import freskog.concurrency.partition.Partition.PartEnv
import org.scalatest.{Assertion, DiagrammedAssertions, FlatSpec}
import scalaz.zio.clock.Clock
import scalaz.zio.console.Console
import scalaz.zio.stm.STM
import scalaz.zio.testkit.{TestClock, TestConsole}
import scalaz.zio._

abstract class BaseTests extends FlatSpec with DiagrammedAssertions {

  val realRts: DefaultRuntime =
    new DefaultRuntime {}

  val clockData:UIO[Ref[TestClock.Data]] =
    Ref.make(TestClock.Zero)

  val consoleData:UIO[Ref[TestConsole.Data]] =
    Ref.make(TestConsole.Data())

  val schedulerData:UIO[Ref[TestClock.Data]] =
    Ref.make(TestClock.Zero)

  val testRts: UIO[TestRuntime] =
    (clockData <*> consoleData <*> schedulerData).map {
      case clockR <*> consoleR <*> schedR => TestRuntime(clockR, consoleR, schedR)
    }

  def partEnv(config:Config):ZIO[Clock with Console, Nothing, PartEnv] =
    ZIO.environment[Clock with Console].map(Partition.buildEnv(config, _))

  def unwrap[R,E,A](zio:ZIO[Clock with Console,E,A]): A =
    realRts.unsafeRun(zio)

  def run(z:ZIO[Clock with Console, Throwable, Assertion]): Assertion = {
    val rts = unwrap(testRts)
    rts.unsafeRunSync(z).getOrElse(c => throw c.squash)
  }

  def runSTM(z:STM[Throwable, Assertion]) =
    run(z.commit)

  def runReal(z:ZIO[Clock with Console, Throwable, Assertion]):Unit = {
    realRts.unsafeRunSync(z).getOrElse(c => throw c.squash)
  }

}
