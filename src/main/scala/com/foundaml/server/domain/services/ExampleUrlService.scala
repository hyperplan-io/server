package com.foundaml.server.domain.services

object ExampleUrlService {

  def correctExampleUrl(predictionId: String, labelId: String) =
    s"/examples?predictionId=$predictionId&labelId=$labelId&isCorrect=true"

  def incorrectExampleUrl(predictionId: String, labelId: String) =
    s"/examples?predictionId=$predictionId&labelId=$labelId&isIncorrect=true"
}
