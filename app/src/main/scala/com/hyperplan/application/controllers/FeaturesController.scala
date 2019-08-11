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
import com.hyperplan.infrastructure.serialization._
import com.hyperplan.infrastructure.serialization.errors._
import com.hyperplan.infrastructure.logging.IOLogging
import com.hyperplan.domain.services.DomainService
import com.hyperplan.domain.errors._

class FeaturesController(domainService: DomainService)
    extends Http4sDsl[IO]
    with IOLogging {

  val unhandledErrorMessage =
    s"""Unhandled server error, please check the logs or contact support"""

  val service: HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case req @ POST -> Root =>
        (for {
          domainClass <- req.as[FeatureVectorDescriptor](
            MonadError[IO, Throwable],
            FeaturesConfigurationSerializer.entityDecoder
          )
          features <- domainService.createFeatures(domainClass).value
          _ <- logger.info(
            s"Features class created with id ${domainClass.id}"
          )
        } yield features)
          .flatMap {
            case Right(domainClass) =>
              Created(FeaturesConfigurationSerializer.encodeJson(domainClass))
            case Left(errors) =>
              BadRequest(
                ErrorsSerializer.encodeJson(errors.toList: _*)
              )
          }
          .handleErrorWith {
            case err =>
              InternalServerError(
                unhandledErrorMessage
              )
          }
      case req @ GET -> Root =>
        domainService.readAllFeatures
          .flatTap(features => logger.debug(s"Read all features $features"))
          .flatMap { features =>
            Ok(FeaturesConfigurationSerializer.encodeJsonList(features))
          }
          .handleErrorWith {
            case err =>
              logger.warn("Unhandled error in FeaturesController", err) >> InternalServerError(
                unhandledErrorMessage
              )
          }
      case req @ GET -> Root / featuresId =>
        domainService
          .readFeatures(featuresId)
          .flatMap {
            case Some(features) =>
              Ok(FeaturesConfigurationSerializer.encodeJson(features))
            case None =>
              NotFound(
                ErrorsSerializer
                  .encodeJson(
                    FeatureVectorDescriptorDoesNotExistError(featuresId)
                  )
              )
          }
          .handleErrorWith {
            case err =>
              InternalServerError(
                unhandledErrorMessage
              )
          }
      case _ @DELETE -> Root / featuresId =>
        domainService.deleteFeatures(featuresId).flatMap {
          case count if count > 0 =>
            Ok()
          case count if count <= 0 =>
            NotFound()
        }
    }
  }

}
