package com.foundaml.server.application.controllers

import com.foundaml.server.application.controllers.requests._
import com.foundaml.server.domain.models.Prediction
import com.foundaml.server.domain.models.errors.LabelNotFound
import com.foundaml.server.domain.services.PredictionsService
import com.foundaml.server.infrastructure.serialization.LabelSerializer
import org.http4s.{HttpRoutes, HttpService}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import scalaz.zio.Task
import scalaz.zio.interop.catz._

class ExamplesController(
    predictionsService: PredictionsService
) extends Http4sDsl[Task] {

  object ValueIdMatcher extends OptionalQueryParamDecoderMatcher[Float]("value")
  object LabelIdMatcher
      extends OptionalQueryParamDecoderMatcher[String]("label")
  object PredictionIdMatcher
      extends QueryParamDecoderMatcher[String]("predictionId")

  val service: HttpRoutes[Task] = {
    HttpRoutes.of[Task] {
      case POST -> Root :? PredictionIdMatcher(predictionId) +& LabelIdMatcher(
            labelId
          ) +& ValueIdMatcher(value) =>
        (for {
          example <- predictionsService.addExample(predictionId, labelId, value)
        } yield example)
          .flatMap { example =>
            Created(LabelSerializer.encodeJson(example))
          }
          .catchAll {
            case LabelNotFound(_) =>
              NotFound(s"The label $labelId does not exist")
          }
    }
  }
}
