package com.foundaml.server.domain.models.labels.transformers

import com.foundaml.server.domain.models.errors.LabelsTransformerError
import com.foundaml.server.domain.models.labels._

case class TensorFlowLabels(result: Set[TensorFlowLabel])

case class TensorFlowLabel(label: String, probability: Float) {
  override def equals(o: Any): Boolean = o match {
    case that: TensorFlowLabel => that.label.equalsIgnoreCase(this.label)
    case _ => false
  }
  override def hashCode: Int = label.toUpperCase.hashCode
}

class TensorFlowLabelsTransformer(fields: Set[String]) {

  def transform(
      tfLabels: TensorFlowLabels
  ): Either[LabelsTransformerError, Labels] = {

    val labelsString = tfLabels.result.map(_.label)
    val probabilities = tfLabels.result.map(_.probability)

    val labels: Set[Label] = tfLabels.result.map { tfLabel =>
      ClassificationLabel(tfLabel.label, tfLabel.probability)
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
