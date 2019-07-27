package com.hyperplan.application.controllers

import cats.Functor
import org.http4s.{HttpRoutes, HttpService}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import cats.effect.IO
import cats.implicits._
import com.hyperplan.application.controllers.requests._
import com.hyperplan.domain.errors.AlgorithmError
import com.hyperplan.domain.errors.AlgorithmError._
import com.hyperplan.domain.services.AlgorithmsService
import com.hyperplan.infrastructure.serialization._
import com.hyperplan.infrastructure.serialization.errors.AlgorithmErrorsSerializer
import com.hyperplan.infrastructure.logging.IOLogging

class AlgorithmsController(
    algorithmsService: AlgorithmsService
) extends Http4sDsl[IO]
    with IOLogging {

  import cats.MonadError
  val service: HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case req @ POST -> Root =>
        (for {
          request <- req.as[PostAlgorithmRequest](
            MonadError[IO, Throwable],
            PostAlgorithmRequestEntitySerializer.entityDecoder
          )
          algorithm <- algorithmsService
            .createAlgorithm(
              request.id,
              request.backend,
              request.projectId,
              request.security
            )
            .value
        } yield algorithm)
          .flatMap {
            case Right(algorithm) =>
              logger.info(
                s"Algorithm created with id ${algorithm.id} on project ${algorithm.projectId}"
              ) *> Created(AlgorithmsSerializer.encodeJson(algorithm))
            case Left(errors) =>
              BadRequest(
                AlgorithmErrorsSerializer.encodeJson(errors.toList: _*)
              )
          }
          .handleErrorWith {
            case AlgorithmAlreadyExistsError(algorithmId) =>
              logger.warn(s"The algorithm $algorithmId already exists") *> Conflict(
                s"Algorithm $algorithmId already exists"
              )
            case IncompatibleFeaturesError(message) =>
              logger.warn(
                s"The features of this algorithm are not compatible with the project"
              ) *> BadRequest(message)
            case IncompatibleLabelsError(message) =>
              logger.warn(
                s"The labels of this algorithm are not compatible with the project"
              ) *> BadRequest(message)
            case err =>
              logger.error(s"Unhandled error", err) *> InternalServerError(
                "Unhandled error"
              )
          }
    }
  }

}
