package com.foundaml.server.application.controllers

import cats.Functor
import org.http4s.{HttpRoutes, HttpService}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import scalaz.zio.Task
import scalaz.zio.interop.catz._
import com.foundaml.server.application.controllers.requests._
import com.foundaml.server.domain.models.errors.{
  AlgorithmAlreadyExists,
  IncompatibleFeatures,
  IncompatibleLabels
}
import com.foundaml.server.domain.services.AlgorithmsService
import com.foundaml.server.infrastructure.serialization._

class AlgorithmsController(
    algorithmsService: AlgorithmsService
) extends Http4sDsl[Task] {

  val service: HttpRoutes[Task] = {
    HttpRoutes.of[Task] {
      case req @ POST -> Root =>
        (for {
          request <- req.as[PostAlgorithmRequest](
            Functor[Task],
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
          .catchAll {
            case AlgorithmAlreadyExists(algorithmId) =>
              Conflict(s"Algorithm $algorithmId already exists")
            case IncompatibleFeatures(message) =>
              BadRequest(message)
            case IncompatibleLabels(message) =>
              BadRequest(message)
          }
    }
  }

}
