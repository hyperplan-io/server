package com.foundaml.server.domain.services

import scalaz.zio.{IO, Task}
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.backends._
import com.foundaml.server.domain.models.errors.{FeaturesValidationFailed, NoAlgorithmAvailable, PredictionError}
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

  def validateFeatures(expectedFeaturesClass: String, features: Features): Boolean = {
    expectedFeaturesClass match {
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
  }

  def predict(
      features: Features,
      project: Project,
      optionalAlgoritmId: Option[String]
  ): Task[Either[PredictionError, Labels]] = {
    if(validateFeatures(project.configuration.featureClass, features)) {
      optionalAlgoritmId.fold(
        predictWithProjectPolicy(features, project)
      )(
        algorithmId =>
          project.algorithmsMap
            .get(algorithmId)
            .fold(
              predictWithProjectPolicy(features, project)
            )(algorithm => predictWithAlgorithm(features, algorithm))
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
