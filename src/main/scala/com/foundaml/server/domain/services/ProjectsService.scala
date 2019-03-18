package com.foundaml.server.domain.services

import com.foundaml.server.application.controllers.requests.PostProjectRequest
import com.foundaml.server.domain.factories.ProjectFactory
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.errors.InvalidArgument
import com.foundaml.server.domain.repositories.ProjectsRepository
import scalaz.zio.{Task, ZIO}

class ProjectsService(
    projectsRepository: ProjectsRepository,
    projectFactory: ProjectFactory
) {

  val regex = "[0-9a-zA-Z-_]*"
  def validateAlphaNumerical(input: String): Option[String] = {
    if (input.matches(regex)) {
      None
    } else {
      Some(s"The identifier $input is not alphanumerical")
    }
  }

  def validateProject(project: Project): List[String] = {
    List(
      validateAlphaNumerical(project.id)
    ).flatten
  }

  def createEmptyProject(
      id: String,
      name: String,
      problem: ProblemType,
      featuresClass: String,
      featuresSize: Int,
      labels: Set[String]
  ): ZIO[Any, Throwable, Project] = {
    val project = Project(
      id,
      name,
      ProjectConfiguration(
        problem,
        featuresClass,
        featuresSize,
        labels
      ),
      Nil,
      NoAlgorithm()
    )
    val errors = validateProject(project)
    for {
      _ <- if (errors.isEmpty) {
        Task(Unit)
      } else {
        Task.fail(
          InvalidArgument(
            errors.mkString(
              s"The following errors occurred: ${errors.mkString(", ")}"
            )
          )
        )
      }
      _ <- projectsRepository.insert(project)
    } yield project
  }

  def readProject(id: String) =
    projectFactory.get(id)

}
