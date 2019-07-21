package com.hyperplan.test.domain.repositories

import com.hyperplan.domain.repositories._
import com.hyperplan.test.{ProjectGenerator, TaskChecker, TestDatabase}
import com.hyperplan.test.{ProjectGenerator, TaskChecker, TestDatabase}
import org.scalatest._

class ProjectsRepositorySpec
    extends FlatSpec
    with Matchers
    with TaskChecker
    with TestDatabase {

  val projectRepository = new ProjectsRepository()(xa)

  it should "insert and read projects correctly" in {
    withInMemoryDatabase { _ =>
      val project = ProjectGenerator.withLocalBackend()
      val insertIO = projectRepository.insertQuery(project)
      val readIO = projectRepository.readQuery(project.id)
      val readAllIO = projectRepository.readAllProjectsQuery
      check(insertIO)
      check(readIO)
      check(readAllIO)
      assert(true)
    }
  }

}
