package com.foundaml.server.application.controllers

import cats.Functor
import org.http4s.{HttpRoutes, HttpService}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import cats.effect.IO
import cats.implicits._
import cats.MonadError
import com.foundaml.server.application.controllers.requests._
import com.foundaml.server.domain.models.errors.{
  AlgorithmAlreadyExists,
  IncompatibleFeatures,
  IncompatibleLabels
}
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.services.AlgorithmsService
import com.foundaml.server.infrastructure.serialization._
import com.foundaml.server.infrastructure.logging.IOLogging
import com.foundaml.server.domain.services.DomainService
import com.foundaml.server.domain.models.errors.LabelsClassDoesNotExist
import com.foundaml.server.domain.models.errors.DomainClassAlreadyExists

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

        } yield domainClass).flatMap { domainClass =>
          Ok(LabelsConfigurationSerializer.encodeJson(domainClass))
        }.handleErrorWith { 
          case err: DomainClassAlreadyExists => 
            Conflict(s"""The labels class ${err.domainClassId} already exists""")
        }
      case req @ GET -> Root =>
        domainService.readAllLabels.flatMap { labels =>
          Ok(LabelsConfigurationSerializer.encodeJsonList(labels)) 
        }
      case req @ GET -> Root / labelsId =>
        domainService.readLabels(labelsId).flatMap { labels => 
          Ok(LabelsConfigurationSerializer.encodeJson(labels)) 
        }.handleErrorWith { 
          case err: LabelsClassDoesNotExist =>
            NotFound(s"""The labels class "$labelsId" does not exist""")
        }
    }
  }

}
