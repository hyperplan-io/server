package com.foundaml.server.domain.models.features.transformers

import com.foundaml.server.domain.models.features._

case class TensorFlowFeaturesTransformer(
    signatureName: String,
    fields: Set[String]
) {

  def transform(
      features: Features
  ): Either[Throwable, TensorFlowClassificationFeatures] = {
    if (features.data.size == fields.size) {

      val examples = features.data.zip(fields).map {
        case (FloatFeature(value), field) =>
          TensorFlowFloatFeature(field, value)
        case (IntFeature(value), field) =>
          TensorFlowIntFeature(field, value)
        case (StringFeature(value), field) =>
          TensorFlowStringFeature(field, value)
      }

      Right(TensorFlowClassificationFeatures(signatureName, examples))
    } else {
      Left(new IllegalArgumentException("Feature transformer failed"))
    }
  }

}
