package com.foundaml.server.domain.models.features.transformers

import com.foundaml.server.domain.models.features.Features.Features
import com.foundaml.server.domain.models.features._

case class TensorFlowFeaturesTransformer(
    signatureName: String,
    fields: Map[String, String]
) {

  def transform(
      features: Features,
      signatureName: String = signatureName,
      fields: Map[String, String] = fields
  ): Either[Throwable, TensorFlowFeatures] = {

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
              case (FloatVectorFeature(key, value), field) =>
                tfFeatures.copy(
                  examples = TensorFlowFloatVectorFeature(field, value) :: tfFeatures.examples
                )
              case (IntVectorFeature(key, value), field) =>
                tfFeatures.copy(
                  examples = TensorFlowIntVectorFeature(field, value) :: tfFeatures.examples
                )
              case (StringVectorFeature(key, value), field) =>
                tfFeatures.copy(
                  examples = TensorFlowStringVectorFeature(field, value) :: tfFeatures.examples
                )
              case (IntVector2dFeature(key, values), field) =>
                tfFeatures.copy(
                  examples = TensorFlowIntVector2dFeature(field, values) :: tfFeatures.examples
                )
              case (FloatVector2dFeature(key, values), field) =>
                tfFeatures.copy(
                  examples = TensorFlowFloatVector2dFeature(field, values) :: tfFeatures.examples
                )
              case (StringVector2dFeature(key, values), field) =>
                tfFeatures.copy(
                  examples = TensorFlowStringVector2dFeature(field, values) :: tfFeatures.examples
                )
              case (ReferenceFeature(key, reference, values), field) =>
                val newFields = values.map { feature =>
                  feature.key -> s"$field\_$feature"
                }.toMap
                tfFeatures.copy(
                  examples = transform(values, signatureName, newFields).toOption.get.examples ::: tfFeatures.examples
                )
            }

        }
    Right(tensorFlowFeatures)
  }

}
