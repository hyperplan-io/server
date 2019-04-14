package com.foundaml.server.application.controllers

import com.foundaml.server.domain.models.errors.LabelNotFound
import com.foundaml.server.domain.services.PredictionsService
import com.foundaml.server.infrastructure.serialization.events.PredictionEventSerializer
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import cats.effect.IO
import cats.implicits._

class ExamplesController(
    predictionsService: PredictionsService
) extends Http4sDsl[IO] {

  object ValueIdMatcher extends OptionalQueryParamDecoderMatcher[Float]("value")
  object LabelIdMatcher
      extends OptionalQueryParamDecoderMatcher[String]("label")
  object PredictionIdMatcher
      extends QueryParamDecoderMatcher[String]("predictionId")

  val service: HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case POST -> Root :? PredictionIdMatcher(predictionId) +& LabelIdMatcher(
            labelId
          ) +& ValueIdMatcher(value) =>
        (for {
          example <- predictionsService.addExample(predictionId, labelId, value)
        } yield example)
          .flatMap { example =>
            Created(PredictionEventSerializer.encodeJson(example))
          }
          .handleErrorWith {
            case LabelNotFound(_) =>
              NotFound(s"The label $labelId does not exist")
          }
    }
  }
}
