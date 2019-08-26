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

package com.hyperplan.infrastructure.serialization.rasa

import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import io.circe.parser._
import io.circe.syntax._

import cats.effect.IO
import cats.implicits._
import com.hyperplan.domain.models.labels.RasaNluClassificationLabels
import com.hyperplan.domain.models.labels.RasaNluIntent

object RasaNluLabelsSerializer {

  import io.circe._
  import io.circe.generic.semiauto._

  implicit val predictionEncoder: Encoder[RasaNluIntent] = deriveEncoder
  implicit val predictionDecoder: Decoder[RasaNluIntent] = deriveDecoder

  implicit val encoder: Encoder[RasaNluClassificationLabels] = deriveEncoder
  implicit val decoder: Decoder[RasaNluClassificationLabels] = deriveDecoder

  implicit val entityDecoder: EntityDecoder[IO, RasaNluClassificationLabels] =
    jsonOf[IO, RasaNluClassificationLabels]

  def encodeJson(project: RasaNluClassificationLabels): Json = {
    project.asJson
  }

  def decodeJson(n: String): RasaNluClassificationLabels = {
    decode[RasaNluClassificationLabels](n).right.get
  }

}
