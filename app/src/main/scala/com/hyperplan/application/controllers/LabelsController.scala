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
import com.hyperplan.domain.errors._
import com.hyperplan.domain.models._
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
          domainClass <- req.as[LabelVectorDescriptor](
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
                ErrorsSerializer
                  .encodeJsonLabels(LabelVectorDescriptorDoesNotExist(labelsId))
              )
          }
          .handleErrorWith {
            case err =>
              logger.warn("Unhandled error in LabelsController", err) >> InternalServerError(
                unhandledErrorMessage
              )
          }
      case _ @DELETE -> Root / labelsId =>
        domainService.deleteLabels(labelsId).flatMap {
          case count if count > 0 =>
            Ok()
          case count if count <= 0 =>
            NotFound()
        }
    }
  }

}
