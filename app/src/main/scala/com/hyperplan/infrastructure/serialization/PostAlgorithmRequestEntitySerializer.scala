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

package com.hyperplan.infrastructure.serialization

import com.hyperplan.application.controllers.requests.PostAlgorithmRequest
import com.hyperplan.domain.models.backends.Backend
import com.hyperplan.domain.models.SecurityConfiguration

import io.circe._
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe.{jsonOf, jsonEncoderOf}

import cats.effect.IO
import cats.implicits._

object PostAlgorithmRequestEntitySerializer {

  implicit val backendDecoder: Decoder[Backend] = BackendSerializer.decoder
  implicit val backendEncoder: Encoder[Backend] = BackendSerializer.encoder
  implicit val securityConfigurationDecoder: Decoder[SecurityConfiguration] =
    (SecurityConfigurationSerializer.decoder)
  implicit val securityConfigurationEncoder: Encoder[SecurityConfiguration] =
    (SecurityConfigurationSerializer.encoder)

  implicit val decoder: Decoder[PostAlgorithmRequest] =
    (c: HCursor) =>
      for {
        backend <- c.downField("backend").as[Backend]
        security <- c.downField("security").as[SecurityConfiguration]
      } yield PostAlgorithmRequest(backend, security)

  implicit val encoder: Encoder[PostAlgorithmRequest] = Encoder.forProduct2(
    "backend",
    "security"
  )(r => (r.backend, r.security))
  implicit val entityDecoder: EntityDecoder[IO, PostAlgorithmRequest] =
    jsonOf[IO, PostAlgorithmRequest]

  implicit val entityEncoder: EntityEncoder[IO, PostAlgorithmRequest] =
    jsonEncoderOf[IO, PostAlgorithmRequest]

}
