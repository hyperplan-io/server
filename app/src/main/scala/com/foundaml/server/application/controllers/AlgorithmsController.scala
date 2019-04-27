package com.foundaml.server.application.controllers

import cats.Functor
import org.http4s.{HttpRoutes, HttpService}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import cats.effect.IO
import cats.implicits._
import com.foundaml.server.application.controllers.requests._
import com.foundaml.server.domain.models.errors.{
  AlgorithmAlreadyExists,
  IncompatibleFeatures,
  IncompatibleLabels
}
import com.foundaml.server.domain.services.AlgorithmsService
import com.foundaml.server.infrastructure.serialization._
import com.foundaml.server.infrastructure.logging.IOLogging

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
          algorithm <- algorithmsService.createAlgorithm(
            request.id,
            request.backend,
            request.projectId
          )
          _ <- logger.info(
            s"Algorithm created with id ${algorithm.id} on project ${algorithm.projectId}"
          )
        } yield algorithm)
          .flatMap { algorithm =>
            Created(AlgorithmsSerializer.encodeJson(algorithm))
          }
          .handleErrorWith {
            case AlgorithmAlreadyExists(algorithmId) =>
              logger.warn(s"The algorithm $algorithmId already exists") *> Conflict(
                s"Algorithm $algorithmId already exists"
              )
            case IncompatibleFeatures(message) =>
              logger.warn(
                s"The features of this algorithm are not compatible with the project"
              ) *> BadRequest(message)
            case IncompatibleLabels(message) =>
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
