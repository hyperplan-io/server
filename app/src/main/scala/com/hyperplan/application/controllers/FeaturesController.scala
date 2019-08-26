/*
 *
 * This file is part of the Hyperplan project.
 * Copyright (C) 2019  Hyperplan
 * Authors: Antoine Sauray
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gn/u.org/licenses/>.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the Hyperplan software without
 * disclosing the source code of your own applications.
 *
 *
 */

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
