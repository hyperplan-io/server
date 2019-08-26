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

import com.hyperplan.application.controllers.requests.PostProjectRequest
import io.circe._
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe.{jsonOf, jsonEncoderOf}
import cats.effect.IO
import cats.implicits._

object PostProjectRequestSerializer {

  import com.hyperplan.domain.models.ProblemType

  implicit val problemTypeEncoder = ProblemTypeSerializer.encoder
  implicit val problemTypeDecoder = ProblemTypeSerializer.decoder

  implicit val decoder: Decoder[PostProjectRequest] =
    (c: HCursor) =>
      for {
        id <- c.downField("id").as[String]
        name <- c.downField("name").as[String]
        problemType <- c.downField("problem").as[ProblemType]
        featuresId <- c.downField("featuresId").as[String]
        labelsId <- c.downField("labelsId").as[Option[String]]
        topic <- c.downField("topic").as[Option[String]]
      } yield
        PostProjectRequest(id, name, problemType, featuresId, labelsId, topic)
  implicit val encoder: Encoder[PostProjectRequest] =
    Encoder.forProduct6(
      "id",
      "name",
      "problem",
      "featuresId",
      "labelsId",
      "topic"
    )(r => (r.id, r.name, r.problem, r.featuresId, r.labelsId, r.topic))

  implicit val entityDecoder: EntityDecoder[IO, PostProjectRequest] =
    jsonOf[IO, PostProjectRequest]

  implicit val entityEncoder: EntityEncoder[IO, PostProjectRequest] =
    jsonEncoderOf[IO, PostProjectRequest]

}
