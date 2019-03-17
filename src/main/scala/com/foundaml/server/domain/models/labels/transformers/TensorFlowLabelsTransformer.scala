package com.foundaml.server.domain.models.labels.transformers

import com.foundaml.server.domain.models.errors.LabelsTransformerError
import com.foundaml.server.domain.models.labels._

case class TensorFlowLabels(result: List[List[(String, Float)]])

case class TensorFlowLabel(label: String, probability: Float) {
  override def equals(o: Any): Boolean = o match {
    case that: TensorFlowLabel => that.label.equalsIgnoreCase(this.label)
    case _ => false
  }
  override def hashCode: Int = label.toUpperCase.hashCode
}

case class TensorFlowLabelsTransformer(fields: Set[String]) {

  def transform(
      tfLabels: TensorFlowLabels
  ): Either[LabelsTransformerError, Labels] = {

    val labelsString = tfLabels.result.flatten.map(_._1).toSet

    val labels: Set[Label] = tfLabels.result.flatten.map {
      case (labelString, probability) =>
        ClassificationLabel(labelString, probability)
    }.toSet

    if (labelsString == fields) {
      Right(Labels(labels))
    } else {
      Left(
        LabelsTransformerError(
          "labels do not exactly match with classification results"
        )
      )
    }
  }
}
