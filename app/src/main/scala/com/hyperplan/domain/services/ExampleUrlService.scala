package com.hyperplan.domain.services

object ExampleUrlService {

  def correctClassificationExampleUrl(predictionId: String, label: String) =
    s"/examples?predictionId=$predictionId&label=$label&isCorrect=true"

  def incorrectClassificationExampleUrl(predictionId: String, label: String) =
    s"/examples?predictionId=$predictionId&label=$label&isIncorrect=true"

  def correctRegressionExampleUrl(predictionId: String) =
    s"/examples?predictionId=$predictionId&isCorrect=true"

  def incorrectRegressionExampleUrl(predictionId: String) =
    s"/examples?predictionId=$predictionId&isIncorrect=true"
}
