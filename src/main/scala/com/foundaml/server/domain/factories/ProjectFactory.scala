package com.foundaml.server.domain.factories

import com.foundaml.server.domain.models.errors.FactoryException
import com.foundaml.server.domain.models.{Project, ProjectConfiguration}
import com.foundaml.server.domain.repositories.{
  AlgorithmsRepository,
  ProjectsRepository
}
import scalaz.zio.Task

object ProjectFactory {
  def apply(
      projectId: String,
      projectsRepository: ProjectsRepository,
      algorithmsRepository: AlgorithmsRepository
  ): Task[Project] = {
    (projectsRepository.read(projectId) zipPar algorithmsRepository
      .readForProject(projectId)).flatMap {
      case (
          (
            id,
            name,
            Right(policy),
            Right(problemType),
            featuresClass,
            featuresSize,
            labels
          ),
          algorithms
          ) =>
        Task.succeed(
          Project(
            id,
            name,
            ProjectConfiguration(
              problemType,
              featuresClass,
              featuresSize,
              labels
            ),
            Nil,
            policy
          )
        )
      case _ =>
        Task.fail(
          FactoryException(
            s"could not build project $projectId because of a SQL error"
          )
        )
    }
  }
}
