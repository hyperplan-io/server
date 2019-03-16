package com.foundaml.server.test.domain.repositories

import com.foundaml.server.domain.repositories.AlgorithmsRepository
import com.foundaml.server.test.utils.{AlgorithmGenerator, TaskChecker, TestDatabase}
import org.scalatest._

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
