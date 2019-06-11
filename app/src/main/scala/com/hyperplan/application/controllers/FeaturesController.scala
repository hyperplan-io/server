package com.hyperplan.application.controllers

import cats.Functor
import org.http4s.{HttpRoutes, HttpService}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import cats.effect.IO
import cats.implicits._
import cats.MonadError
import com.hyperplan.application.controllers.requests._
import com.hyperplan.domain.models.errors.{
  AlgorithmAlreadyExists,
  IncompatibleFeatures,
  IncompatibleLabels
}
import com.hyperplan.domain.models._
import com.hyperplan.domain.services.AlgorithmsService
import com.hyperplan.infrastructure.serialization._
import com.hyperplan.infrastructure.logging.IOLogging
import com.hyperplan.domain.services.DomainService
import com.hyperplan.domain.models.errors.FeaturesClassDoesNotExist
import com.hyperplan.domain.models.errors.DomainClassAlreadyExists

class FeaturesController(domainService: DomainService)
    extends Http4sDsl[IO]
    with IOLogging {

  val service: HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case req @ POST -> Root =>
        (for {
          domainClass <- req.as[FeaturesConfiguration](
            MonadError[IO, Throwable],
            FeaturesConfigurationSerializer.entityDecoder
          )
          _ <- domainService.createFeatures(domainClass)
          _ <- logger.info(
            s"Domain class created with id ${domainClass.id}"
          )

        } yield domainClass)
          .flatMap { domainClass =>
            Ok(FeaturesConfigurationSerializer.encodeJson(domainClass))
          }
          .handleErrorWith {
            case err: DomainClassAlreadyExists =>
              Conflict(
                s"""The features class ${err.domainClassId} already exists"""
              )
          }
      case req @ GET -> Root =>
        domainService.readAllFeatures.flatMap { features =>
          Ok(FeaturesConfigurationSerializer.encodeJsonList(features))
        }
      case req @ GET -> Root / featuresId =>
        domainService
          .readFeatures(featuresId)
          .flatMap { features =>
            Ok(FeaturesConfigurationSerializer.encodeJson(features))
          }
          .handleErrorWith {
            case err: FeaturesClassDoesNotExist =>
              NotFound(s"""The features class "$featuresId" does not exist""")
          }
    }
  }

}
