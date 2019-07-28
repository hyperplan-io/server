package com.hyperplan.application.controllers

import cats.effect.IO
import cats.implicits._
import cats.Functor
import cats.MonadError

import org.http4s.{HttpRoutes, HttpService}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

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

  val unhandledErrorMessage =
    s"""Unhandled server error, please check the logs or contact support"""
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
              val errorList = errors.toList
              logger.warn(
                s"Failed to create algorithm: ${errorList.mkString(",")}"
              ) *> BadRequest(
                AlgorithmErrorsSerializer.encodeJson(errorList: _*)
              )
          }
          .handleErrorWith { err =>
            logger.warn(s"Unhandled error in AlgorithmsController", err) *> InternalServerError(
              "An unknown error occurred"
            )
          }
    }
  }

}
