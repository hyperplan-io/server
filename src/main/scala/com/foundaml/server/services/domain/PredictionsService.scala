package com.foundaml.server.services.domain

import scalaz.zio. { IO, Task }

import com.foundaml.server.models.backends._
import com.foundaml.server.models._
import com.foundaml.server.models.features._

class PredictionsService {

  def predictWithProjectPolicy(
      features: Features,
      project: Project
  ) =
    project.policy.take().fold(
      ??? //IO.fail(new IllegalStateException(""))
      )(algorithm =>predictWithAlgorithm(
        features,
        algorithm
      )
  )

  def predictWithAlgorithm(
      features: Features,
      algorithm: Algorithm
  ) = algorithm.backend match {
    case local: Local =>
      IO(local.computed)
  }

  def predict(
      features: Features,
      project: Project,
      optionalAlgoritmId: Option[String]
  ) =
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
