package com.foundaml.server.domain.models.errors

class TensorFlowBackendError(message: String) extends Throwable(message)

case class LabelsTransformerError(message: String)
    extends TensorFlowBackendError(message)
case class FeaturesTransformerError(message: String)
    extends TensorFlowBackendError(message)
