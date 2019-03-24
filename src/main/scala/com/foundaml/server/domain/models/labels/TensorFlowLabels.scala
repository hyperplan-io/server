package com.foundaml.server.domain.models.labels

case class TensorFlowClassificationLabel(label: String, probability: Float) {
  override def equals(o: Any): Boolean = o match {
    case that: TensorFlowClassificationLabel => that.label.equalsIgnoreCase(this.label)
    case _ => false
  }
  override def hashCode: Int = label.toUpperCase.hashCode
}

case class TensorFlowClassificationLabels(result: List[List[(String, Float)]])
case class TensorFlowRegressionLabels(result: List[Float])
