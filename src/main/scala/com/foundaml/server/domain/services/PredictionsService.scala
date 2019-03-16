package com.foundaml.server.domain.services

import scalaz.zio.{IO, Task}
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.backends._
import com.foundaml.server.domain.models.errors.{FeaturesValidationFailed, LabelsValidationFailed, NoAlgorithmAvailable, PredictionError}
import com.foundaml.server.domain.models.features._
import com.foundaml.server.domain.models.labels._
import com.foundaml.server.domain.repositories.ProjectsRepository

class PredictionsService(projectsRepository: ProjectsRepository) {

  def noAlgorithm(): Task[Either[PredictionError, Labels]] =
    Task(Left(NoAlgorithmAvailable("No algorithms are setup")))

  def predictWithProjectPolicy(
      features: Features,
      project: Project
  ): Task[Either[PredictionError, Labels]] =
    project.policy
      .take()
      .fold(
        noAlgorithm()
      ) { algorithmId =>
        project.algorithmsMap
          .get(algorithmId)
          .fold(
            noAlgorithm()
          )(
            algorithm =>
              predictWithAlgorithm(
                features,
                algorithm
              )
          )
      }

  def predictWithAlgorithm(
      features: Features,
      algorithm: Algorithm
  ): Task[Either[PredictionError, Labels]] = algorithm.backend match {
    case local: Local =>
      Task(Right(local.computed))
    case tfBackend @ TensorFlowBackend(host, port, fTransormer, lTransformer) =>
      Task(
        Left(NoAlgorithmAvailable("Tensorflow backend is not implemented yet"))
      )
  }

  def validateFeatures(expectedFeaturesClass: String, expectedFeaturesSize: Int, features: Features): Boolean = {
    lazy val typeCheck = expectedFeaturesClass match {
      case DoubleFeatures.featuresClass =>
        features.features.count(_.isInstanceOf[DoubleFeature]) == features.features.size
      case FloatFeatures.featuresClass =>
        features.features.count(_.isInstanceOf[FloatFeatures]) == features.features.size
      case IntFeatures.featuresClass =>
        features.features.count(_.isInstanceOf[IntFeatures]) == features.features.size
      case StringFeatures.featuresClass =>
        features.features.count(_.isInstanceOf[StringFeatures]) == features.features.size
      case CustomFeatures.featuresClass =>
        // custom features does not guarantee the features to be correct
        true
    }
    lazy val sizeCheck = features.features.size == expectedFeaturesSize

    sizeCheck && typeCheck
  }

  def validateLabels(expectedLabelsClass: Set[String], labels: Labels): Boolean = {
    expectedLabelsClass == labels.labels.map(_.label)
  }

  def predict(
      features: Features,
      project: Project,
      optionalAlgoritmId: Option[String]
  ): Task[Either[PredictionError, Labels]] = {
    if(validateFeatures(project.configuration.featureClass, project.configuration.featuresSize, features)) {
      optionalAlgoritmId.fold(
        predictWithProjectPolicy(features, project)
      )(
        algorithmId =>
          project.algorithmsMap
            .get(algorithmId)
            .fold(
              predictWithProjectPolicy(features, project)
            )(algorithm =>
              predictWithAlgorithm(features, algorithm).map { predictionResults =>
                predictionResults.flatMap { labels =>
                  if(validateLabels(project.configuration.labels, labels)) {
                    Right(labels)
                  } else {
                    Left(LabelsValidationFailed("The labels do not match the project configuration"))
                  }
                }
              }
            )
      )
    } else {
      Task(
        Left(
          FeaturesValidationFailed(
            "The features are not correct for this project"
          )
        )
      )
    }
  }
}
