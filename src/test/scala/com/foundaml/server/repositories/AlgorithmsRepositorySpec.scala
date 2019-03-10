package com.foundaml.server.repositories

import scalaz.zio. { IO, Task }
import scalaz.zio.interop.catz._

import doobie.scalatest.imports._
import org.scalatest._
import org.scalatest.Inside.inside

import com.foundaml.server.services.infrastructure.storage._
import com.foundaml.server.repositories._
import doobie.imports._

import com.foundaml.server.utils._
import com.foundaml.server.models._

class AlgorithmsRepositorySpec extends FlatSpec with Matchers with TaskChecker {
 
  val xa = Transactor.fromDriverManager[Task](
    "org.postgresql.Driver", "jdbc:postgresql://127.0.0.1:5432/postgres", "postgres", "postgres"
  )

  def transactor = xa

  val algorithmRepository = new AlgorithmsRepository()(xa)

  it should "insert and read algorithm correctly" in {
    val algorithm = AlgorithmGenerator.withLocalBackend() 
    val insertIO = algorithmRepository.insert(algorithm)
    val readIO = algorithmRepository.read(algorithm.id)
    val readForProjectIO = algorithmRepository.readForProject(algorithm.projectId)
    val readAllIO = algorithmRepository.readAll()
    check(insertIO)
    check(readIO)
    check(readForProjectIO)
    check(readAllIO)
  }

}
