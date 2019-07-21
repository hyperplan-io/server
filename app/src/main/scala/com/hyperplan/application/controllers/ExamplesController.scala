package com.hyperplan.application.controllers

import com.hyperplan.domain.errors._
import com.hyperplan.domain.services.PredictionsService
import com.hyperplan.infrastructure.serialization.events.PredictionEventSerializer
import com.hyperplan.infrastructure.logging.IOLogging
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import cats.effect.IO
import cats.implicits._

class ExamplesController(
    predictionsService: PredictionsService
) extends Http4sDsl[IO]
    with IOLogging {

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
          _ <- logger.info(s"Example assigned to prediction $predictionId")
        } yield example)
          .flatMap { example =>
            Created(PredictionEventSerializer.encodeJson(example))
          }
          .handleErrorWith {
            case LabelNotFound(_) =>
              NotFound(s"The label $labelId does not exist")
            case err =>
              logger.error(s"Unhandled error: ${err.getMessage}") *> InternalServerError(
                "unknown error"
              )
          }
    }
  }
}
