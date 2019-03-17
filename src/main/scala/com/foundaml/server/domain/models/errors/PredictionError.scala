package com.foundaml.server.domain.models.errors

class PredictionError(message: String) extends Throwable(message)

case class NoAlgorithmAvailable(message: String) extends PredictionError(message)
case class FeaturesValidationFailed(message: String) extends PredictionError(message)
case class LabelsValidationFailed(message: String) extends PredictionError(message)
