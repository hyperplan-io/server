package com.hyperplan.application.controllers

import cats.Functor
import org.http4s.{HttpRoutes, HttpService}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import cats.effect.IO
import cats.implicits._
import cats.MonadError
import com.hyperplan.application.controllers.requests._
import com.hyperplan.domain.errors._
import com.hyperplan.domain.models._
import com.hyperplan.domain.services.AlgorithmsService
import com.hyperplan.infrastructure.serialization._
import com.hyperplan.infrastructure.logging.IOLogging
import com.hyperplan.domain.services.DomainService
import com.hyperplan.infrastructure.serialization.errors._

class LabelsController(domainService: DomainService)
    extends Http4sDsl[IO]
    with IOLogging {

  val unhandledErrorMessage =
    s"""Unhandled server error, please check the logs or contact support"""

  val service: HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case req @ POST -> Root =>
        (for {
          domainClass <- req.as[LabelsConfiguration](
            MonadError[IO, Throwable],
            LabelsConfigurationSerializer.entityDecoder
          )
          labels <- domainService.createLabels(domainClass).value
          _ <- logger.info(
            s"Domain class created with id ${domainClass.id}"
          )

        } yield labels)
          .flatMap {
            case Right(labels) =>
              Created(LabelsConfigurationSerializer.encodeJson(labels))
            case Left(errors) =>
              BadRequest(
                ErrorsSerializer.encodeJsonLabels(errors.toList: _*)
              )
          }
          .handleErrorWith {
            case err =>
              logger.warn("Unhandled error in LabelsController", err) >> InternalServerError(
                unhandledErrorMessage
              )
          }

      case req @ GET -> Root =>
        domainService.readAllLabels
          .flatTap(labels => logger.debug(s"Read all labels $labels"))
          .flatMap { labels =>
            Ok(LabelsConfigurationSerializer.encodeJsonList(labels))
          }
          .handleErrorWith {
            case err =>
              logger.warn("Unhandled error in LabelsController", err) >> InternalServerError(
                unhandledErrorMessage
              )
          }
      case req @ GET -> Root / labelsId =>
        domainService
          .readLabels(labelsId)
          .flatMap {
            case Some(labels) =>
              Ok(LabelsConfigurationSerializer.encodeJson(labels))
            case None =>
              NotFound(
                ErrorsSerializer.encodeJsonLabels(LabelsDoesNotExist(labelsId))
              )
          }
          .handleErrorWith {
            case err =>
              logger.warn("Unhandled error in LabelsController", err) >> InternalServerError(
                unhandledErrorMessage
              )
          }
    }
  }

}
