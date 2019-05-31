package freskog.concurrency

import scalaz.zio.{UIO, ZIO}

package object partition extends Partition.Service[Partition] {

  type PartId = String

  final val partitionService:ZIO[Partition, Nothing, Partition.Service[Any]] =
    ZIO.access(_.partition)

  final def partition[A](config:Config, partIdOf:A => PartId, action: A => UIO[Unit]):ZIO[Partition, Nothing, A => UIO[Boolean]] =
    ZIO.accessM[Partition](_.partition.partition(config,partIdOf, action))

}
