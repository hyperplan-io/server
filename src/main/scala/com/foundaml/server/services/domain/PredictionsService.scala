package com.foundaml.server.services.domain

import scalaz.zio. { IO, Task }

import com.foundaml.server.models.backends._
import com.foundaml.server.models._
import com.foundaml.server.models.features._
import com.foundaml.server.models.labels._

import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._


class PredictionsService {

  def noAlgorithm(): Task[Labels] = Task.fail(new Exception("No algorithms are setup"))

  def predictWithProjectPolicy(
      features: Features,
      project: Project
  ): Task[Labels] =
    project.policy.take().fold(
      noAlgorithm()
      ){algorithm => 
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
    case TensorFlowBackend(host, port, transformer) =>
      val tfFeatures = transformer.transform(features)
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
