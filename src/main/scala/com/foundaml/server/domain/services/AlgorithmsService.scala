package com.foundaml.server.domain.services

import scalaz.zio.{Task, ZIO}

import com.foundaml.server.domain.factories.ProjectFactory
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.backends.Backend
import com.foundaml.server.domain.models.errors.InvalidArgument
import com.foundaml.server.domain.models.features.transformers.TensorFlowFeaturesTransformer
import com.foundaml.server.domain.models.labels.transformers.TensorFlowLabelsTransformer
import com.foundaml.server.domain.repositories.{
  AlgorithmsRepository,
  ProjectsRepository
}

class AlgorithmsService(
    algorithmsRepository: AlgorithmsRepository,
    projectsRepository: ProjectsRepository,
    projectFactory: ProjectFactory
) {

  def validateEqualSize(
      expectedSize: Int,
      actualSize: Int,
      featureName: String
  ) =
    if (expectedSize != actualSize) {
      Some(s"The $featureName size is incorrect for the project")
    } else {
      None
    }

  def validate(algorithm: Algorithm, project: Project) = {
    algorithm.backend match {
      case com.foundaml.server.domain.models.backends.Local(computed) => Nil
      case com.foundaml.server.domain.models.backends.TensorFlowBackend(
          _,
          _,
          TensorFlowFeaturesTransformer(signatureName, fields),
          TensorFlowLabelsTransformer(labels)
          ) =>
        val size = project.configuration.features match {
          case FeaturesConfiguration(
              featuresClasses: List[FeatureConfiguration]
              ) =>
            featuresClasses.size
        }
        List(
          validateEqualSize(
            size,
            fields.size,
            "features"
          ),
          validateEqualSize(
            project.configuration.labels.size,
            labels.size,
            "labels"
          )
        ).flatten
    }
  }

  def createAlgorithm(id: String, backend: Backend, projectId: String) = {
    for {
      project <- projectFactory.get(projectId)
      algorithm = Algorithm(
        id,
        backend,
        projectId
      )
      errors = validate(algorithm, project)
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
      insertResult <- algorithmsRepository.insert(algorithm)
      result <- insertResult.fold(
        err => Task.fail(err),
        _ => Task.succeed(algorithm)
      )
    } yield result
  }
}
