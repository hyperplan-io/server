package com.foundaml.server.domain.models.errors

sealed trait PredictionError extends Throwable {
  def message: String
}

case class NoAlgorithmAvailable(message: String) extends PredictionError
case class FeaturesValidationFailed(message: String) extends PredictionError
case class LabelsValidationFailed(message: String) extends PredictionError
