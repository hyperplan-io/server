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

import cats.effect.IO
import cats.implicits._

import io.circe._
import io.circe.syntax._
import io.circe.parser.decode

import org.http4s.circe.{jsonOf, jsonEncoderOf}

import com.hyperplan.domain.models._
import com.hyperplan.domain.models.backends.Backend
import org.http4s.{EntityDecoder, EntityEncoder}

object AlgorithmsSerializer {

  implicit val encoder: Encoder[Algorithm] =
    (algorithm: Algorithm) =>
      Json.obj(
        "id" -> Json.fromString(algorithm.id),
        "projectId" -> Json.fromString(algorithm.projectId),
        "backend" -> BackendSerializer.encodeJson(algorithm.backend),
        "security" -> SecurityConfigurationSerializer.encodeJson(
          algorithm.security
        )
      )

  implicit val decoder: Decoder[Algorithm] =
    (c: HCursor) =>
      for {
        id <- c.downField("id").as[String]
        projectId <- c.downField("projectId").as[String]
        backend <- c.downField("backend").as[Backend](BackendSerializer.decoder)
        security <- c
          .downField("security")
          .as[SecurityConfiguration](SecurityConfigurationSerializer.decoder)
      } yield Algorithm(id, backend, projectId, security)

  def encodeJson(algorithm: Algorithm): Json = {
    algorithm.asJson
  }

  def decodeJson(n: String): Either[io.circe.Error, Algorithm] = {
    decode[Algorithm](n)
  }

  implicit val entityDecoder: EntityDecoder[IO, Algorithm] =
    jsonOf[IO, Algorithm]

  implicit val entityEncoder: EntityEncoder[IO, Algorithm] =
    jsonEncoderOf[IO, Algorithm]
}
