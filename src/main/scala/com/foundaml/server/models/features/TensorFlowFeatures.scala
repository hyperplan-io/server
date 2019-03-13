package com.foundaml.server.models.features

sealed trait TensorFlowFeature
case class TensorFlowDoubleFeature(key: String, value: Double) extends TensorFlowFeature
case class TensorFlowFloatFeature(key: String, value: Float) extends TensorFlowFeature
case class TensorFlowIntFeature(key: String, value: Int) extends TensorFlowFeature
case class TensorFlowStringFeature(key: String, value: String) extends TensorFlowFeature



case class TensorFlowClassificationFeatures(signatureName: String, examples: List[TensorFlowFeature])

