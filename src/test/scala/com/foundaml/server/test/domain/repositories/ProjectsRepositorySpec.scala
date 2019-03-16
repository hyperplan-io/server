package com.foundaml.server.test.domain.repositories

import com.foundaml.server.domain.repositories._
import com.foundaml.server.test.utils.{ProjectGenerator, TaskChecker, TestDatabase}
import org.scalatest._

class ProjectsRepositorySpec
    extends FlatSpec
    with Matchers
    with TaskChecker
    with TestDatabase {

  val projectRepository = new ProjectsRepository()(xa)

  it should "insert project correctly" in {

    withInMemoryDatabase { _ =>
      val project = ProjectGenerator.withLocalBackend()
      val insertIO = projectRepository.insertQuery(project)
      val readIO = projectRepository.readQuery(project.id)
      val readAllIO = projectRepository.readAll()
      check(insertIO)
      check(readIO)
      check(readAllIO)
    }
  }

}
