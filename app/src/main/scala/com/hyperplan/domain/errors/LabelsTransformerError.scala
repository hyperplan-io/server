package com.hyperplan.domain.errors

sealed trait TensorFlowLabelsTransformerError extends Throwable

sealed trait RasaNluLabelsTransformerError extends Throwable

sealed trait BasicLabelsTransformerError extends Throwable

case class RasaNluMissingLabelError() extends RasaNluLabelsTransformerError
case class BasicLabelMissingError() extends BasicLabelsTransformerError
