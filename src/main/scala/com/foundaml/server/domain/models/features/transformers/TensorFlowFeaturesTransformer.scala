package com.foundaml.server.domain.models.features.transformers

import com.foundaml.server.domain.models.features.Features.Features
import com.foundaml.server.domain.models.features._

case class TensorFlowFeaturesTransformer(
    signatureName: String,
    fields: Set[String]
) {

  def transform(
      features: Features
  ): Either[Throwable, TensorFlowFeatures] = {
    if (features.size == fields.size) {

      val examples = features.zip(fields).map {
        case (FloatFeature(value), field) =>
          TensorFlowFloatFeature(field, value)
        case (IntFeature(value), field) =>
          TensorFlowIntFeature(field, value)
        case (StringFeature(value), field) =>
          TensorFlowStringFeature(field, value)
        case (FloatVectorFeature(value), field) =>
          TensorFlowFloatVectorFeature(field, value)
        case (IntVectorFeature(value), field) =>
          TensorFlowIntVectorFeature(field, value)
        case (StringVectorFeature(value), field) =>
          TensorFlowStringVectorFeature(field, value)
      }

      Right(TensorFlowFeatures(signatureName, examples))
    } else {
      Left(new IllegalArgumentException("Feature transformer failed"))
    }
  }

}
