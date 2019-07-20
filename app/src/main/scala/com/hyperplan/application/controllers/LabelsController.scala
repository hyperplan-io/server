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

class LabelsController(domainService: DomainService)
    extends Http4sDsl[IO]
    with IOLogging {

  val service: HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case req @ POST -> Root =>
        (for {
          domainClass <- req.as[LabelsConfiguration](
            MonadError[IO, Throwable],
            LabelsConfigurationSerializer.entityDecoder
          )
          _ <- domainService.createLabels(domainClass)
          _ <- logger.info(
            s"Domain class created with id ${domainClass.id}"
          )

        } yield domainClass)
          .flatMap { domainClass =>
            Ok(LabelsConfigurationSerializer.encodeJson(domainClass))
          }
          .handleErrorWith {
            case err: DomainClassAlreadyExists =>
              Conflict(
                s"""The labels class ${err.domainClassId} already exists"""
              )
          }
      case req @ GET -> Root =>
        domainService.readAllLabels.flatMap { labels =>
          Ok(LabelsConfigurationSerializer.encodeJsonList(labels))
        }
      case req @ GET -> Root / labelsId =>
        domainService
          .readLabels(labelsId)
          .flatMap { labels =>
            Ok(LabelsConfigurationSerializer.encodeJson(labels))
          }
          .handleErrorWith {
            case err: LabelsClassDoesNotExist =>
              NotFound(s"""The labels class "$labelsId" does not exist""")
          }
    }
  }

}
