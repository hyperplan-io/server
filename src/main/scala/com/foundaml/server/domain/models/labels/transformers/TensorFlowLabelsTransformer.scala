package com.foundaml.server.domain.models.labels.transformers

import com.foundaml.server.domain.models.errors.LabelsTransformerError
import com.foundaml.server.domain.models.labels._

case class TensorFlowLabels(result: List[TensorFlowLabel])
case class TensorFlowLabel(label: String, probability: Float)

class TensorFlowLabelsTransformer(fields: List[String]) {

  def transform(
      tfLabels: TensorFlowLabels
  ): Either[LabelsTransformerError, Labels] = {
    val labels = tfLabels.result.map { tfLabel =>
      ClassificationLabel(tfLabel.label, tfLabel.probability)
    }
    if(labels.map(_.label) == fields) {
      Right(Labels(labels))
    } else {
      Left(LabelsTransformerError("labels do not exactly match with classification results"))
    }
  }
}
