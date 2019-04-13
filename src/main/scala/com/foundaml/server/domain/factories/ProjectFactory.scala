package com.foundaml.server.domain.factories

import com.foundaml.server.domain.models.errors.{
  FactoryException,
  ProjectDataInconsistent
}
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.repositories.{
  AlgorithmsRepository,
  ProjectsRepository
}
import com.foundaml.server.infrastructure.logging.IOLogging
import cats.effect.IO
import cats.implicits._

class ProjectFactory(
    projectsRepository: ProjectsRepository,
    algorithmsRepository: AlgorithmsRepository
) extends IOLogging {
  def get(projectId: String): IO[Either[ProjectDataInconsistent, Project]] = {
    (
      projectsRepository.read(projectId),
      algorithmsRepository.readForProject(projectId)
    ).mapN {
      case (
          (
            id,
            name,
            Right(Classification),
            Right(policy),
            Right(projectConfiguration: ClassificationConfiguration)
          ),
          algorithms
          ) =>
        Right(
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
            Right(Regression),
            Right(policy),
            Right(projectConfiguration: RegressionConfiguration)
          ),
          algorithms
          ) =>
        Right(
          RegressionProject(
            id,
            name,
            projectConfiguration,
            algorithms,
            policy
          )
        )
      case projectData =>
        Left(ProjectDataInconsistent(projectId))
    }
  }
}
