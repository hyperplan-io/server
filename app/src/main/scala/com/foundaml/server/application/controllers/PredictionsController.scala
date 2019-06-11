package com.foundaml.server.application.controllers

import cats.Functor
import org.http4s.{HttpRoutes, HttpService}
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import cats.effect.IO
import cats.implicits._
import cats.MonadError

import com.foundaml.server.application.AuthenticationMiddleware
import com.foundaml.server.application.controllers.requests._
import com.foundaml.server.domain.models.errors._
import com.foundaml.server.domain.services.{
  DomainService,
  PredictionsService,
  ProjectsService
}
import com.foundaml.server.infrastructure.logging.IOLogging
import com.foundaml.server.infrastructure.serialization.{
  PredictionRequestEntitySerializer,
  PredictionSerializer
}
import com.foundaml.server.domain.services.FeaturesParserService

import java.nio.charset.StandardCharsets

import io.circe.Json
import io.circe._, io.circe.parser._

class PredictionsController(
    projectsService: ProjectsService,
    domainService: DomainService,
    predictionsService: PredictionsService
) extends Http4sDsl[IO]
    with IOLogging {

  implicit val implicitDomainService = domainService
  val service: HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case req @ POST -> Root =>
        (for {
          predictionRequest <- req.as[PredictionRequest](
            MonadError[IO, Throwable],
            PredictionRequestEntitySerializer.requestDecoder
          )
          project <- projectsService.readProject(predictionRequest.projectId)
          body <- req.body.compile.toList
          jsonBody <- IO.fromEither(
            parse(new String(body.toArray, StandardCharsets.UTF_8))
          )
          features <- FeaturesParserService.parseFeatures(
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
            s"Prediction computed for project ${prediction.projectId} using algorithm ${prediction.algorithmId}"
          )
        } yield prediction)
          .flatMap { prediction =>
            Created(
              PredictionSerializer.encodeJson(prediction)
            )
          }
          .handleErrorWith {
            case AlgorithmDoesNotExist(algorithmId) =>
              NotFound(s"the algorithm $algorithmId does not exist")
            case PredictionAlreadyExist(predictionId) =>
              Conflict(s"The prediction $predictionId already exists")
            case BackendError(message) =>
              logger.warn(s"An error occurred in prediction: $message") *> InternalServerError(
                message
              )
            case FeaturesValidationFailed(message) =>
              logger.warn(s"The features could not be validated") *> FailedDependency(
                message
              )
            case LabelsValidationFailed(message) =>
              logger.warn(s"The labels could not be validated") *> FailedDependency(
                message
              )
            case NoAlgorithmAvailable(message) =>
              logger.warn(s"No algorithms are available") *> FailedDependency(
                message
              )
            case FeaturesTransformerError(message) =>
              logger.warn(
                s"The features could not be transformed to a backend api compatible format"
              ) *> FailedDependency(message)
            case LabelsTransformerError(message) =>
              logger.warn(
                s"The labels could not be transformed to a backend api compatible format"
              ) *> FailedDependency(message)
            case err: InvalidMessageBodyFailure =>
              logger.warn("An error occured with json body", err) *> BadRequest(
                "Json payload is not correct"
              )
            case err =>
              logger.error(s"Unhandled error", err) *> InternalServerError(
                "unknown error"
              )
          }
    }
  }

}
