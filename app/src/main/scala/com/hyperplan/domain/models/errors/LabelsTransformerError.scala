package com.hyperplan.domain.models.errors

sealed trait TensorFlowLabelsTransformerError extends Throwable

sealed trait RasaNluLabelsTransformerError extends Throwable

case class RasaNluMissingLabelError() extends RasaNluLabelsTransformerError
