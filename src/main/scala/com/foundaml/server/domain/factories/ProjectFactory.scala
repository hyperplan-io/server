package com.foundaml.server.domain.factories

import com.foundaml.server.domain.models.errors.{FactoryException, ProjectDataInconsistent}
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.repositories.{AlgorithmsRepository, ProjectsRepository}
import scalaz.zio.{Task, ZIO}

class ProjectFactory(
    projectsRepository: ProjectsRepository,
    algorithmsRepository: AlgorithmsRepository
) {
  def get(projectId: String): ZIO[Any, Throwable, Project] =
    (projectsRepository.read(projectId) zipPar algorithmsRepository
      .readForProject(projectId)).flatMap {
      case (
          (
            id,
            name,
            Right(Classification()),
            Right(policy),
            Right(projectConfiguration: ClassificationConfiguration)
          ),
          algorithms
          ) =>
        Task.succeed(
          ClassificationProject(
            id,
            name,
            projectConfiguration,
            algorithms,
            policy
          )
        )
      case (
        (
          id,
          name,
          Right(Regression()),
          Right(policy),
          Right(projectConfiguration: RegressionConfiguration)
          ),
        algorithms
        ) =>
        Task.succeed(
          RegressionProject(
            id,
            name,
            projectConfiguration,
            algorithms,
            policy
          )
        )
      case _ =>
        Task.fail(ProjectDataInconsistent(projectId))
    }
}
