package com.hyperplan.domain.models.features.transformers

import com.hyperplan.domain.models.features.Features.Features
import com.hyperplan.domain.models.features._

case class TensorFlowFeaturesTransformer(
    signatureName: String,
    fields: Map[String, String]
) {

  def transform(
      features: Features,
      signatureName: String = signatureName,
      fields: Map[String, String] = fields
  ): TensorFlowFeatures = {

    val tensorFlowFeatures =
      features
        .flatMap { feature =>
          fields.get(feature.key).map { newKey =>
            feature -> newKey
          }
        }
        .foldLeft(TensorFlowFeatures(signatureName, List.empty)) {
          case (tfFeatures, feature) =>
            feature match {
              case (FloatFeature(key, value), field) =>
                tfFeatures.copy(
                  examples = TensorFlowFloatFeature(field, value) :: tfFeatures.examples
                )
              case (IntFeature(key, value), field) =>
                tfFeatures.copy(
                  examples = TensorFlowIntFeature(field, value) :: tfFeatures.examples
                )
              case (StringFeature(key, value), field) =>
                tfFeatures.copy(
                  examples = TensorFlowStringFeature(field, value) :: tfFeatures.examples
                )
              case (FloatArrayFeature(key, value), field) =>
                tfFeatures.copy(
                  examples = TensorFlowFloatVectorFeature(field, value) :: tfFeatures.examples
                )
              case (IntArrayFeature(key, value), field) =>
                tfFeatures.copy(
                  examples = TensorFlowIntVectorFeature(field, value) :: tfFeatures.examples
                )
              case (StringArrayFeature(key, value), field) =>
                tfFeatures.copy(
                  examples = TensorFlowStringVectorFeature(field, value) :: tfFeatures.examples
                )
              case (IntMatrixFeature(key, values), field) =>
                tfFeatures.copy(
                  examples = TensorFlowIntVector2dFeature(field, values) :: tfFeatures.examples
                )
              case (FloatMatrixFeature(key, values), field) =>
                tfFeatures.copy(
                  examples = TensorFlowFloatVector2dFeature(field, values) :: tfFeatures.examples
                )
              case (StringMatrixFeature(key, values), field) =>
                tfFeatures.copy(
                  examples = TensorFlowStringVector2dFeature(field, values) :: tfFeatures.examples
                )
              case (ReferenceFeature(key, reference, values), field) =>
                val newFields = values.map { feature =>
                  feature.key -> s"$field\_$feature"
                }.toMap
                tfFeatures.copy(
                  examples = transform(values, signatureName, newFields).examples ::: tfFeatures.examples
                )
            }

        }
    tensorFlowFeatures
  }

}
