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

import com.hyperplan.application.controllers.requests.PredictionRequest

import com.hyperplan.domain.models.EntityLink
import com.hyperplan.domain.models.features.Features
import com.hyperplan.domain.models.features.Features.Features

import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

object PredictionRequestSerializer {

  import io.circe.generic.semiauto._

  implicit val entityLinkEncoder: Encoder[EntityLink] =
    PredictionSerializer.entityLinkEncoder
  implicit val entityLinkDecoder: Decoder[EntityLink] =
    PredictionSerializer.entityLinkDecoder

  implicit val encoder: Encoder[PredictionRequest] = deriveEncoder
  implicit val decoder: Decoder[PredictionRequest] = deriveDecoder

  def encodeJson(request: PredictionRequest): String = {
    request.asJson.noSpaces
  }

  def decodeJson(n: String): PredictionRequest = {
    decode[PredictionRequest](n).right.get
  }
}
