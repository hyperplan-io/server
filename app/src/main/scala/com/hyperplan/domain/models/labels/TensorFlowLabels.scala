package com.hyperplan.domain.models.labels

case class TensorFlowClassificationLabel(label: String, score: Float) {
  override def equals(o: Any): Boolean = o match {
    case that: TensorFlowClassificationLabel =>
      that.label.equalsIgnoreCase(this.label)
    case _ => false
  }
  override def hashCode: Int = label.toUpperCase.hashCode
}
case class TensorFlowClassificationLabels(
    result: List[TensorFlowClassificationLabel]
)
case class TensorFlowRegressionLabels(result: List[List[Float]])
