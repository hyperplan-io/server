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

import com.hyperplan.domain.services.PrivacyService
import com.hyperplan.application.controllers.requests.ForgetPredictionRequest
import com.hyperplan.application.controllers.responses.ForgetPredictionsResponse
import com.hyperplan.infrastructure.logging.IOLogging
import com.hyperplan.infrastructure.serialization._

class PrivacyController(
    privacyService: PrivacyService
) extends Http4sDsl[IO]
    with IOLogging {

  val service: HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case req @ POST -> Root / "gdpr" =>
        (for {
          request <- req.as[ForgetPredictionRequest](
            MonadError[IO, Throwable],
            ForgetPredictionRequestSerializer.entityDecoder
          )
          noForgotten <- privacyService.forgetPredictionsLinkedToEntity(
            request.entityName,
            request.entityId
          )
        } yield
          ForgetPredictionsResponse(
            request.entityName,
            request.entityId,
            noForgotten
          ))
          .flatMap { response =>
            Ok(ForgetPredictionsResponseSerializer.encodeJson(response))
          }
          .handleErrorWith {
            case err =>
              logger.error(s"Unhandled error: ${err.getMessage}") *> IO
                .raiseError(err)
          }
    }
  }
}
