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
case class TensorFlowFloatVectorFeature(key: String, value: List[Float])
    extends TensorFlowFeature
case class TensorFlowIntVectorFeature(key: String, value: List[Int])
    extends TensorFlowFeature
case class TensorFlowStringVectorFeature(key: String, value: List[String])
    extends TensorFlowFeature
case class TensorFlowEmptyVectorFeature(key: String) extends TensorFlowFeature
case class TensorFlowIntVector2dFeature(key: String, value: List[List[Int]])
    extends TensorFlowFeature
case class TensorFlowFloatVector2dFeature(key: String, value: List[List[Float]])
    extends TensorFlowFeature
case class TensorFlowStringVector2dFeature(
    key: String,
    value: List[List[String]]
) extends TensorFlowFeature

case class TensorFlowFeatures(
    signatureName: String,
    examples: List[TensorFlowFeature]
)
