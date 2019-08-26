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

import io.circe.generic.extras.Configuration
import io.circe._
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import org.http4s.circe.jsonEncoderOf
import com.hyperplan.domain.models.labels._
import org.http4s.{EntityDecoder, EntityEncoder}
import cats.effect.IO
import cats.implicits._

object LabelsSerializer {

  import io.circe.generic.extras.semiauto._

  implicit val discriminator: Configuration =
    Configuration.default.withDiscriminator("class")

  implicit val labelEncoder: Encoder[Label] = LabelSerializer.encoder
  implicit val labelDecoder: Decoder[Label] = LabelSerializer.decoder

  implicit val encoder: Encoder[Labels] = deriveEncoder[Labels]
  implicit val decoder: Decoder[Labels] = deriveDecoder[Labels]

  def encodeJson(labels: Labels): Json = {
    labels.asJson
  }

  def encodeJsonNoSpaces(labels: Labels): String = {
    labels.asJson.noSpaces
  }

  def decodeJson(n: String): Labels = {
    decode[Labels](n).right.get
  }
}

object LabelSerializer {
  import io.circe.generic.extras.Configuration
  import io.circe.generic.extras.semiauto._

  implicit val discriminator: Configuration =
    Configuration.default.withDiscriminator("class")

  implicit val encoder: Encoder[Label] = deriveEncoder
  implicit val decoder: Decoder[Label] = deriveDecoder

  implicit val labelEntityDecoder: EntityEncoder[IO, Label] =
    jsonEncoderOf[IO, Label]

  def encodeJsonNoSpaces(labels: Label): String = {
    labels.asJson.noSpaces
  }

  def encodeJson(labels: Label): Json = {
    labels.asJson
  }

  def encodeJsonSet(labels: Set[Label]): Json = {
    labels.asJson
  }

  def encodeJsonSetNoSpaces(labels: Set[Label]): String = {
    labels.asJson.noSpaces
  }

  def decodeJson(n: String): Either[io.circe.Error, Label] = {
    decode[Label](n)
  }

  def decodeJsonSet(n: String): Either[io.circe.Error, Set[Label]] = {
    decode[Set[Label]](n)
  }
}
