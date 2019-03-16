package com.foundaml.server.domain.models.errors

sealed trait TensorFlowBackendError extends Throwable

case class LabelsTransformerError(message: String)
case class FeaturesTransformerError(message: String)
