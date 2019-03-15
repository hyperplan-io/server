package com.foundaml.server.domain.models.features.transformers

import com.foundaml.server.domain.models.features._

class TensorFlowFeaturesTransformer(
    signatureName: String,
    fields: List[String]
) {

  def transform(
      features: Features
  ): Either[Throwable, TensorFlowClassificationFeatures] = {
    if (features.features.length == fields.length) {
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
