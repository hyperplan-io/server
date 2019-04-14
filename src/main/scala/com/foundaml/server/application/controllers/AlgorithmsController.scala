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

  val service: HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case req @ POST -> Root =>
        (for {
          request <- req.as[PostAlgorithmRequest](
            Functor[IO],
            PostAlgorithmRequestEntitySerializer.entityDecoder
          )
          algorithm <- algorithmsService.createAlgorithm(
            request.id,
            request.backend,
            request.projectId
          )
        } yield algorithm)
          .flatMap { algorithm =>
            Created(AlgorithmsSerializer.encodeJson(algorithm))
          }
          .handleErrorWith {
            case AlgorithmAlreadyExists(algorithmId) =>
              Conflict(s"Algorithm $algorithmId already exists")
            case IncompatibleFeatures(message) =>
              BadRequest(message)
            case IncompatibleLabels(message) =>
              BadRequest(message)
            case err =>
              logger.error(s"Unhandled error: ${err.getMessage}") *> IO
                .raiseError(err)
          }
    }
  }

}
