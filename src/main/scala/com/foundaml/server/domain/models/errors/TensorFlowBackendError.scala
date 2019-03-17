package com.foundaml.server.domain.models.errors

sealed trait TensorFlowBackendError extends Throwable

case class LabelsTransformerError(message: String)
    extends TensorFlowBackendError
case class FeaturesTransformerError(message: String)
    extends TensorFlowBackendError
