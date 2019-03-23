package com.foundaml.server.domain.models.features

sealed trait TensorFlowFeature {
  val key: String
}
case class TensorFlowFloatFeature(key: String, value: Float)
    extends TensorFlowFeature
case class TensorFlowIntFeature(key: String, value: Int)
    extends TensorFlowFeature
case class TensorFlowStringFeature(key: String, value: String)
    extends TensorFlowFeature

case class TensorFlowClassificationFeatures(
    signatureName: String,
    examples: List[TensorFlowFeature]
)