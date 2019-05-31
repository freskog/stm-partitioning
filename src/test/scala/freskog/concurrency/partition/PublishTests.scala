package freskog.concurrency.partition

import freskog.concurrency.partition.Partition.publish
import scalaz.zio.stm.TQueue

class PublishTests extends BaseTests {

  behavior of "a publisher"

  it should "return true when publishing to an empty TQueue" in {
    runSTM {
      (TQueue.make[Int](1) >>= (publish(_, 1))) map (published => assert(published))
    }
  }

  it should "return false when publishing to a full TQueue" in {
    runSTM(
      (TQueue.make[Int](0) >>= (publish(_, 1))) map (published => assert(!published))
    )
  }
}