package com.foundaml.server.application.controllers

import com.foundaml.server.application.controllers.requests._
import com.foundaml.server.domain.models.Prediction
import com.foundaml.server.domain.services.PredictionsService
import com.foundaml.server.infrastructure.serialization.{PredictionRequestEntitySerializer, PredictionSerializer}
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import scalaz.zio.Task
import scalaz.zio.interop.catz._

class ExamplesController(
    predictionsService: PredictionsService
) extends Http4sDsl[Task] {

  object LabelIdMatcher extends QueryParamDecoderMatcher[String]("labelId")
  object PredictionIdMatcher extends QueryParamDecoderMatcher[String]("predictionId")

  val service: HttpService[Task] = {
    HttpService[Task] {
      case req @ POST -> Root :? PredictionIdMatcher(predictionId) +& LabelIdMatcher(labelId) =>
        (for {
          example <- predictionsService.addExample(predictionId, labelId)
        } yield ()).flatMap { example =>
          Ok()
        }
    }
  }

  def predict(
      request: PredictionRequest
  ): Task[Prediction] = {
    predictionsService.predict(
      request.projectId,
      request.features,
      request.algorithmId
    )

  }

}
