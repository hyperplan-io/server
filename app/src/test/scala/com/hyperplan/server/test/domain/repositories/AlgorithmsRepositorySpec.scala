package com.hyperplan.server.test.domain.repositories

import com.hyperplan.domain.repositories.AlgorithmsRepository
import com.hyperplan.server.test.{AlgorithmGenerator, TaskChecker, TestDatabase}
import org.scalatest._

class AlgorithmsRepositorySpec
    extends FlatSpec
    with Matchers
    with TaskChecker
    with TestDatabase {

  val algorithmRepository = new AlgorithmsRepository()(xa)

  it should "insert and read algorithms correctly" in {

    withInMemoryDatabase { _ =>
      val algorithm = AlgorithmGenerator.withLocalBackend()
      val insertIO = algorithmRepository.insertQuery(algorithm)
      val readIO = algorithmRepository.readQuery(algorithm.id)
      val readForProjectIO =
        algorithmRepository.readForProjectQuery(algorithm.projectId)
      val readAllIO = algorithmRepository.readAllQuery()
      check(insertIO)
      check(readIO)
      check(readForProjectIO)
      check(readAllIO)
    }
  }

}
