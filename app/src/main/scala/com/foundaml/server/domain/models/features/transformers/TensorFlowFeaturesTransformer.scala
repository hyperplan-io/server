package com.foundaml.server.domain.models.features.transformers

import com.foundaml.server.domain.models.features.Features.Features
import com.foundaml.server.domain.models.features._

case class TensorFlowFeaturesTransformer(
    signatureName: String,
    fields: Set[String]
) {

  def transform(
      features: Features,
      signatureName: String = signatureName,
      fields: Set[String] = fields
  ): Either[Throwable, TensorFlowFeatures] = {
    val tensorFlowFeatures = features
      .zip(fields)
      .foldLeft(TensorFlowFeatures(signatureName, List.empty)) {
        case (tfFeatures, feature) =>
          feature match {
            case (FloatFeature(value), field) =>
              tfFeatures.copy(
                examples = TensorFlowFloatFeature(field, value) :: tfFeatures.examples
              )
            case (IntFeature(value), field) =>
              tfFeatures.copy(
                examples = TensorFlowIntFeature(field, value) :: tfFeatures.examples
              )
            case (StringFeature(value), field) =>
              tfFeatures.copy(
                examples = TensorFlowStringFeature(field, value) :: tfFeatures.examples
              )
            case (FloatVectorFeature(value), field) =>
              tfFeatures.copy(
                examples = TensorFlowFloatVectorFeature(field, value) :: tfFeatures.examples
              )
            case (IntVectorFeature(value), field) =>
              tfFeatures.copy(
                examples = TensorFlowIntVectorFeature(field, value) :: tfFeatures.examples
              )
            case (StringVectorFeature(value), field) =>
              tfFeatures.copy(
                examples = TensorFlowStringVectorFeature(field, value) :: tfFeatures.examples
              )
            case (EmptyVectorFeature, field) =>
              tfFeatures.copy(
                examples = TensorFlowEmptyVectorFeature(field) :: tfFeatures.examples
              )
            case (IntVector2dFeature(values), field) =>
              tfFeatures.copy(
                examples = TensorFlowIntVector2dFeature(field, values) :: tfFeatures.examples
              )
            case (FloatVector2dFeature(values), field) =>
              tfFeatures.copy(
                examples = TensorFlowFloatVector2dFeature(field, values) :: tfFeatures.examples
              )
            case (StringVector2dFeature(values), field) =>
              tfFeatures.copy(
                examples = TensorFlowStringVector2dFeature(field, values) :: tfFeatures.examples
              )
            case (ReferenceFeature(values), field) =>
              val newFields = values.map { feature =>
                s"$field\_$feature"
              }.toSet
              tfFeatures.copy(
                examples = transform(values, signatureName, newFields).toOption.get.examples ::: tfFeatures.examples
              )
            case (EmptyVector2dFeature, field) =>
              tfFeatures.copy(
                examples = TensorFlowEmptyVectorFeature(field) :: tfFeatures.examples
              )
          }

      }
    Right(tensorFlowFeatures)
  }

}
