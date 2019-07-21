package com.hyperplan.application.controllers

import cats.Functor
import org.http4s.{HttpRoutes, HttpService}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import cats.effect.IO
import cats.implicits._
import cats.MonadError
import com.hyperplan.application.controllers.requests._
import com.hyperplan.domain.models._
import com.hyperplan.domain.services.AlgorithmsService
import com.hyperplan.infrastructure.serialization._
import com.hyperplan.infrastructure.logging.IOLogging
import com.hyperplan.domain.services.DomainService
import com.hyperplan.domain.errors._

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
          features <- domainService.createFeatures(domainClass).value
          _ <- logger.info(
            s"Domain class created with id ${domainClass.id}"
          )

        } yield features)
          .flatMap {
            case Right(domainClass) =>
              Ok(FeaturesConfigurationSerializer.encodeJson(domainClass))
            case Left(err) =>
              ???
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
          .flatMap {
            case Some(features) =>
              Ok(FeaturesConfigurationSerializer.encodeJson(features))
            case None =>
              NotFound("")
          }
          .handleErrorWith {
            case err: FeaturesClassDoesNotExist =>
              NotFound(s"""The features class "$featuresId" does not exist""")
          }
    }
  }

}
