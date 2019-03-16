package com.foundaml.server.domain.models.features.transformers

import com.foundaml.server.domain.models.features._

case class TensorFlowFeaturesTransformer(
    signatureName: String,
    fields: Set[String]
) {

  def transform(
      features: Features
  ): Either[Throwable, TensorFlowClassificationFeatures] = {
    if (features.features.size == fields.size) {
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
      Right(TensorFlowClassificationFeatures(signatureName, examples))
    } else {
      Left(new IllegalArgumentException(""))
    }
  }

}
