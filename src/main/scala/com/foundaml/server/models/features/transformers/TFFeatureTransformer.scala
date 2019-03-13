package com.foundaml.server.models.features.transformers

import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

import com.foundaml.server.models.features._

class TensorFlowFeaturesTransformer(fields: List[String]) {

  def transform(features: Features): TensorFlowClassificationFeatures =  {
    val signatureName = ""

    val examples = features.features.zip(fields).map {
      case (DoubleFeature(value), field) =>
        TensorFlowDoubleFeature(field, value)
      case (FloatFeature(value), field) =>
        TensorFlowFloatFeature(field, value)
      case (IntFeature(value), field) =>
        TensorFlowIntFeature(field, value)
      case (StringFeature(value), field) =>
        TensorFlowStringFeature(field, value)
    }
    TensorFlowClassificationFeatures(signatureName, examples)
  } 

  def toJson(tfFeatures: TensorFlowClassificationFeatures) = tfFeatures.asJson
}
