package com.foundaml.server.domain.factories

import com.foundaml.server.domain.models.errors.{
  FactoryException,
  ProjectDataInconsistent
}
import com.foundaml.server.domain.models.{Project, ProjectConfiguration}
import com.foundaml.server.domain.repositories.{
  AlgorithmsRepository,
  ProjectsRepository
}
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
            Right(policy),
            Right(problemType),
            Right(featuresConfiguration),
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
              featuresConfiguration,
              labels
            ),
            algorithms,
            policy
          )
        )
      case _ =>
        Task.fail(ProjectDataInconsistent(projectId))
    }
}
