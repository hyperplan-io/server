package com.foundaml.server.domain.services

import scalaz.zio.{IO, Task}
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.backends._
import com.foundaml.server.domain.models.errors.{
  NoAlgorithmAvailable,
  PredictionError
}
import com.foundaml.server.domain.models.features._
import com.foundaml.server.domain.models.labels._

class PredictionsService {

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

  def predict(
      features: Features,
      project: Project,
      optionalAlgoritmId: Option[String]
  ): Task[Either[PredictionError, Labels]] =
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

}
