package com.hyperplan.application.controllers

import cats.effect.IO
import cats.implicits._
import cats.MonadError
import cats.Functor
import io.circe.Json
import io.circe._
import io.circe.parser._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import com.hyperplan.application.AuthenticationMiddleware
import com.hyperplan.application.controllers.requests._
import com.hyperplan.domain.errors._
import com.hyperplan.domain.services._
import com.hyperplan.infrastructure.logging.IOLogging
import com.hyperplan.infrastructure.serialization.{
  PredictionRequestEntitySerializer,
  PredictionSerializer
}
import com.hyperplan.domain.services.FeaturesParserService
import java.nio.charset.StandardCharsets

import com.hyperplan.domain.errors.PredictionError._
import com.hyperplan.domain.models.features.Features.Features
import com.hyperplan.domain.models.{Prediction, Project, ProjectConfiguration}
import com.hyperplan.infrastructure.serialization.errors.PredictionErrorsSerializer

class PredictionsController(
    projectsService: ProjectsService,
    domainService: DomainService,
    predictionsService: PredictionsService
) extends Http4sDsl[IO]
    with IOLogging {

  val unhandledErrorMessage =
    s"""Unhandled server error, please check the logs or contact support"""

  val parseFeatures: (ProjectConfiguration, Json) => IO[Features] =
    FeaturesParserService.parseFeatures(domainService)

  val service: HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case req @ POST -> Root =>
        (for {
          predictionRequest <- req.as[PredictionRequest](
            MonadError[IO, Throwable],
            PredictionRequestEntitySerializer.requestDecoder
          )
          optProject <- projectsService.readProject(predictionRequest.projectId)
          eitherProject <- optProject
            .fold[IO[Either[PredictionError, Prediction]]](
              IO.pure(
                ProjectDoesNotExistError(
                  ProjectDoesNotExistError.message(predictionRequest.projectId)
                ).asLeft
              )
            ) { project =>
              for {
                body <- req.body.compile.toList
                jsonBody <- IO.fromEither(
                  parse(new String(body.toArray, StandardCharsets.UTF_8))
                )
                features <- parseFeatures(
                  project.configuration,
                  jsonBody
                )
                prediction <- predictionsService.predict(
                  predictionRequest.projectId,
                  features,
                  predictionRequest.entityLinks.getOrElse(Nil),
                  predictionRequest.algorithmId
                )
                _ <- logger.debug(
                  s"Prediction computed for project ${predictionRequest.projectId} using algorithm ${predictionRequest.algorithmId}"
                )
              } yield prediction
            }
        } yield eitherProject)
          .flatMap {
            case Right(prediction) =>
              Created(
                PredictionSerializer.encodeJson(prediction)
              )
            case Left(error: BackendExecutionError) =>
              BadGateway(
                PredictionErrorsSerializer.encodeJson(error)
              )
            case Left(error) =>
              BadRequest(
                PredictionErrorsSerializer.encodeJson(error)
              )
          }
          .handleErrorWith { err =>
            logger.warn("Unhandled error in PredictionsController", err) >> InternalServerError(
              unhandledErrorMessage
            )
          }

    }
  }

}
