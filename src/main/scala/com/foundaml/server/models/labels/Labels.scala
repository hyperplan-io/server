package com.foundaml.server.models.labels

sealed trait Labels

case class TensorFlowClassicationLabel(
    classIds: List[Int],
    probabilities: List[Float],
    classes: List[String],
    logits: List[Float]
)

case class TensorFlowClassificationLabels(
    predictions: List[TensorFlowClassicationLabel]
) extends Labels
