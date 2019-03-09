package com.foundaml.server.services.domain

import com.foundaml.server.models.backends._
import com.foundaml.server.models._

class PredictionsService {

  def predictWithProjectPolicy[FeatureType, LabelType](
    features: FeatureType,
    project: Project[FeatureType, LabelType]
  ) = ???


  def predictWithAlgorithm[FeatureType, LabelType](
    features: FeatureType,
    algorithm: Algorithm[FeatureType, LabelType]
  ) = algorithm.backend match {
    case local: Local[FeatureType, LabelType] =>
      local.compute(features)
  }

  def predict[FeatureType, LabelType](
    features: FeatureType,
    project: Project[FeatureType, LabelType],
    optionalAlgoritmId: Option[String]
  ) = optionalAlgoritmId.fold(
      predictWithProjectPolicy(features, project)
    )(algorithmId => 
      project.algorithms.get(algorithmId).fold(
        predictWithProjectPolicy(features, project)
        )(algorithm => predictWithAlgorithm(features, algorithm))
    )

}
