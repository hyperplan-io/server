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
        case (EmptyVectorFeature, field) =>
          TensorFlowEmptyVectorFeature(field)
        case (IntVector2dFeature(values), field) =>
          TensorFlowIntVector2dFeature(field, values)
        case (FloatVector2dFeature(values), field) =>
          TensorFlowFloatVector2dFeature(field, values)
        case (StringVector2dFeature(values), field) =>
          TensorFlowStringVector2dFeature(field, values)
        case (EmptyVector2dFeature, field) =>
          TensorFlowEmptyVectorFeature(field)

      }

      Right(TensorFlowFeatures(signatureName, examples))
    } else {
      Left(new IllegalArgumentException("Feature transformer failed"))
    }
  }

}
