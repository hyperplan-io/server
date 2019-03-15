package com.foundaml.server.domain.repositories

import scalaz.zio.{IO, Task}
import scalaz.zio.interop.catz._

import doobie.scalatest.imports._
import org.scalatest._
import org.scalatest.Inside.inside

import com.foundaml.server.infrastructure.storage._
import com.foundaml.server.domain.repositories._
import doobie.imports._

import com.foundaml.server.utils._

class ProjectsRepositorySpec extends FlatSpec with Matchers with TaskChecker {

  val xa = Transactor.fromDriverManager[Task](
    "org.postgresql.Driver",
    "jdbc:postgresql://127.0.0.1:5432/postgres",
    "postgres",
    "postgres"
  )

  def transactor = xa

  val projectRepository = new ProjectsRepository()(xa)

  it should "insert project correctly" in {
    val project = ProjectGenerator.withLocalBackend()
    val insertIO = projectRepository.insertQuery(project)
    val readIO = projectRepository.readQuery(project.id)
    val readAllIO = projectRepository.readAll()
    check(insertIO)
    check(readIO)
    check(readAllIO)
  }

}
