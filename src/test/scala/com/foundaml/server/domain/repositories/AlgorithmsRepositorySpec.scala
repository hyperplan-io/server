package com.foundaml.server.domain.repositories

import com.foundaml.server.utils._
import doobie.imports._
import org.scalatest._
import scalaz.zio.Task
import scalaz.zio.interop.catz._

class AlgorithmsRepositorySpec
    extends FlatSpec
    with Matchers
    with TaskChecker
    with TestDatabase {

  val algorithmRepository = new AlgorithmsRepository()(xa)

  it should "insert and read algorithm correctly" in {

    withInMemoryDatabase { _ =>
      val algorithm = AlgorithmGenerator.withLocalBackend()
      val insertIO = algorithmRepository.insert(algorithm)
      val readIO = algorithmRepository.read(algorithm.id)
      val readForProjectIO =
        algorithmRepository.readForProject(algorithm.projectId)
      val readAllIO = algorithmRepository.readAll()
      check(insertIO)
      check(readIO)
      check(readForProjectIO)
      check(readAllIO)
    }

  }

}
