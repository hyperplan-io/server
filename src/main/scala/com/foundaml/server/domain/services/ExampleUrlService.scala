package com.foundaml.server.domain.services

object ExampleUrlService {

  def correctClassificationExampleUrl(predictionId: String, labelId: String) =
    s"/examples?predictionId=$predictionId&labelId=$labelId&isCorrect=true"

  def incorrectClassificationExampleUrl(predictionId: String, labelId: String) =
    s"/examples?predictionId=$predictionId&labelId=$labelId&isIncorrect=true"

  def correctRegressionExampleUrl(predictionId: String) =
    s"/examples?predictionId=$predictionId&isCorrect=true"

  def incorrectRegressionExampleUrl(predictionId: String) =
    s"/examples?predictionId=$predictionId&isIncorrect=true"
}
