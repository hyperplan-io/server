package com.foundaml.server.domain.models.errors

sealed trait PredictionError {
  def message: String
}

case class NoAlgorithmAvailable(message: String) extends PredictionError
