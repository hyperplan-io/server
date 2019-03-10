package com.foundaml.server.models.labels

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._
import io.circe.generic.JsonCodec

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

object Labels {

 implicit val genDevConfig: Configuration = Configuration.default.withDiscriminator("what_am_i")
}
