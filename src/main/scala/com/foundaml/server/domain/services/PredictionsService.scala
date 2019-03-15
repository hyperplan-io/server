package com.foundaml.server.domain.services

import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.backends._
import com.foundaml.server.domain.models.features._
import com.foundaml.server.domain.models.labels._
import scalaz.zio.{IO, Task}

class PredictionsService {

  def noAlgorithm(): Task[Labels] =
    Task.fail(new Exception("No algorithms are setup"))

  def predictWithProjectPolicy(
      features: Features,
      project: Project
  ): Task[Labels] =
    project.policy
      .take()
      .fold(
        noAlgorithm()
      ) { algorithm =>
        predictWithAlgorithm(
          features,
          algorithm
        )
      }

  def predictWithAlgorithm(
      features: Features,
      algorithm: Algorithm
  ): Task[Labels] = algorithm.backend match {
    case local: Local =>
      IO(local.computed)
    case tfBackend @ TensorFlowBackend(host, port, fTransormer) =>
      ???

  }

  def predict(
      features: Features,
      project: Project,
      optionalAlgoritmId: Option[String]
  ): Task[Labels] =
    optionalAlgoritmId.fold(
      predictWithProjectPolicy(features, project)
    )(
      algorithmId =>
        project.algorithms
          .get(algorithmId)
          .fold(
            predictWithProjectPolicy(features, project)
          )(algorithm => predictWithAlgorithm(features, algorithm))
    )

}
