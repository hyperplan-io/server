package com.hyperplan.domain.services

import cats.effect.IO
import cats.implicits._
import com.hyperplan.domain.models._
import com.hyperplan.domain.models.backends.{
  Backend,
  LocalClassification,
  TensorFlowClassificationBackend,
  TensorFlowRegressionBackend
}
import com.hyperplan.domain.models.errors.{
  IncompatibleAlgorithm,
  IncompatibleFeatures,
  IncompatibleLabels,
  InvalidArgument
}
import com.hyperplan.domain.models.features.transformers.TensorFlowFeaturesTransformer
import com.hyperplan.domain.models.labels.transformers.TensorFlowLabelsTransformer
import com.hyperplan.domain.repositories.{
  AlgorithmsRepository,
  ProjectsRepository
}
import com.hyperplan.infrastructure.logging.IOLogging
import com.hyperplan.domain.models.backends.RasaNluClassificationBackend
import com.hyperplan.domain.models.errors.AlgorithmError

class AlgorithmsService(
    projectsService: ProjectsService,
    algorithmsRepository: AlgorithmsRepository,
    projectsRepository: ProjectsRepository
) extends IOLogging {

  def validateFeaturesConfiguration(
      expectedSize: Int,
      actualSize: Int,
      featureName: String
  ) =
    if (expectedSize != actualSize) {
      Some(
        IncompatibleFeatures(
          s"The features dimension is incorrect for the project"
        )
      )
    } else {
      None
    }

  def validateLabelsConfiguration(
      labels: Map[String, String],
      labelsConfiguration: LabelsConfiguration
  ) = labelsConfiguration.data match {
    case OneOfLabelsConfiguration(oneOf, _) =>
      if (labels.size != oneOf.size) {
        Some(
          IncompatibleLabels(
            s"The labels dimension is incorrect for the project"
          )
        )
      } else {
        None
      }
    case DynamicLabelsConfiguration(description) =>
      None
  }

  def validateClassificationAlgorithm(
      algorithm: Algorithm,
      project: ClassificationProject
  ): List[AlgorithmError] = {
    algorithm.backend match {
      case LocalClassification(computed) => Nil
      case TensorFlowClassificationBackend(
          _,
          _,
          TensorFlowFeaturesTransformer(signatureName, fields),
          TensorFlowLabelsTransformer(labels)
          ) =>
        val size = project.configuration.features match {
          case FeaturesConfiguration(
              id,
              featuresClasses: List[FeatureConfiguration]
              ) =>
            featuresClasses.size
        }
        List(
          validateFeaturesConfiguration(
            size,
            fields.size,
            "features"
          ),
          validateLabelsConfiguration(
            labels,
            project.configuration.labels
          )
        ).flatten
      case TensorFlowRegressionBackend(_, _, _) =>
        List(IncompatibleAlgorithm(algorithm.id))
      case RasaNluClassificationBackend(_, _, _, _) =>
        Nil
    }

  }

  def validateRegressionAlgorithm(
      algorithm: Algorithm,
      project: RegressionProject
  ) = {
    algorithm.backend match {
      case LocalClassification(computed) => Nil
      case TensorFlowRegressionBackend(
          _,
          _,
          TensorFlowFeaturesTransformer(signatureName, fields)
          ) =>
        val size = project.configuration.features match {
          case FeaturesConfiguration(
              id,
              featuresClasses: List[FeatureConfiguration]
              ) =>
            featuresClasses.size
        }
        List(
          validateFeaturesConfiguration(
            size,
            fields.size,
            "features"
          )
        ).flatten
      case TensorFlowClassificationBackend(_, _, _, _) =>
        List(IncompatibleAlgorithm(algorithm.id))
      case RasaNluClassificationBackend(_, _, _, _) =>
        ???
    }
  }

  def createAlgorithm(
      id: String,
      backend: Backend,
      projectId: String,
      security: SecurityConfiguration
  ) = {
    val algorithm = Algorithm(
      id,
      backend,
      projectId,
      security
    )
    projectsService
      .readProject(projectId)
      .flatMap(
        project => {
          val errors = project match {
            case classificationProject: ClassificationProject =>
              validateClassificationAlgorithm(
                algorithm,
                classificationProject
              )
            case regressionProject: RegressionProject =>
              validateRegressionAlgorithm(algorithm, regressionProject)
          }
          for {
            _ <- if (errors.isEmpty) {
              IO.unit
            } else {
              val message =
                s"The following errors occurred: ${errors.mkString(", ")}"
              logger.warn(message) *> IO.raiseError(
                InvalidArgument(message)
              )
            }
            insertResult <- algorithmsRepository.insert(algorithm)
            result <- insertResult.fold(
              err => {
                IO.raiseError(err)
              },
              _ => IO.pure(algorithm)
            )
            _ <- if (project.algorithms.isEmpty) {
              project match {
                case classificationProject: ClassificationProject =>
                  projectsService.updateProject(
                    projectId,
                    None,
                    Some(DefaultAlgorithm(id))
                  )
                case regressionProject: RegressionProject =>
                  projectsService.updateProject(
                    projectId,
                    None,
                    Some(DefaultAlgorithm(id))
                  )

              }
            } else {
              IO.unit
            }
          } yield result
        }
      )
  }
}
