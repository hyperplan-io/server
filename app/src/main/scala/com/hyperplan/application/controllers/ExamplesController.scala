package com.hyperplan.application.controllers

import cats.effect.IO
import cats.implicits._

import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

import com.hyperplan.domain.errors.PredictionError
import com.hyperplan.domain.errors.PredictionError._
import com.hyperplan.domain.services.PredictionsService
import com.hyperplan.infrastructure.serialization.events.PredictionEventSerializer
import com.hyperplan.infrastructure.logging.IOLogging
import com.hyperplan.infrastructure.serialization.errors.PredictionErrorsSerializer

class ExamplesController(
    predictionsService: PredictionsService
) extends Http4sDsl[IO]
    with IOLogging {

  object ValueIdMatcher extends OptionalQueryParamDecoderMatcher[Float]("value")
  object LabelIdMatcher
      extends OptionalQueryParamDecoderMatcher[String]("label")
  object PredictionIdMatcher
      extends QueryParamDecoderMatcher[String]("predictionId")

  val unhandledErrorMessage =
    s"""Unhandled server error, please check the logs or contact support"""

  val service: HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case POST -> Root :? PredictionIdMatcher(predictionId) +& LabelIdMatcher(
            labelId
          ) +& ValueIdMatcher(value) =>
        (for {
          example <- predictionsService.addExample(predictionId, labelId, value)
          _ <- logger.info(s"Example assigned to prediction $predictionId")
        } yield example)
          .flatMap {
            case Right(example) =>
              Created(PredictionEventSerializer.encodeJson(example))
            case Left(error) =>
              BadRequest(
                PredictionErrorsSerializer.encodeJson(error)
              )
          }
          .handleErrorWith {
            err =>
              logger.error(s"Unhandled error in ExamplesController: ${err.getMessage}") *> InternalServerError(
                unhandledErrorMessage
              )
          }
    }
  }
}
