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

package com.hyperplan.infrastructure.serialization.labels

import com.hyperplan.domain.models.labels.ClassificationLabel
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import org.http4s.circe.jsonEncoderOf
import io.circe._

object ClassificationLabelSerializer {
  implicit val classificationlabelEncoder: Encoder[ClassificationLabel] =
    (label: ClassificationLabel) =>
      Json.obj(
        ("label", Json.fromString(label.label)),
        ("probability", Json.fromFloatOrNull(label.probability)),
        ("correctExampleUrl", Json.fromString(label.correctExampleUrl)),
        ("incorrectExampleUrl", Json.fromString(label.incorrectExampleUrl))
      )

  implicit val classificationLabelDecoder: Decoder[ClassificationLabel] =
    (c: HCursor) =>
      for {
        label <- c.downField("label").as[String]
        probability <- c.downField("probability").as[Float]
        correctExampleUrl <- c.downField("correctExampleUrl").as[String]
        incorrectExampleUrl <- c.downField("incorrectExampleUrl").as[String]
      } yield
        ClassificationLabel(
          label,
          probability,
          correctExampleUrl,
          incorrectExampleUrl
        )

  implicit val classificationLabelsSetEncoder
      : ArrayEncoder[Set[ClassificationLabel]] =
    Encoder.encodeSet[ClassificationLabel]
  implicit val classificationLabelsSetDecoder
      : Decoder[Set[ClassificationLabel]] =
    Decoder.decodeSet[ClassificationLabel]

  def encodeJsonNoSpaces(labels: ClassificationLabel): String = {
    labels.asJson.noSpaces
  }

  def encodeJson(labels: ClassificationLabel): Json = {
    labels.asJson
  }

  def encodeJsonSet(labels: Set[ClassificationLabel]): Json = {
    labels.asJson
  }

  def encodeJsonSetNoSpaces(labels: Set[ClassificationLabel]): String = {
    labels.asJson.noSpaces
  }

  def decodeJson(n: String): Either[io.circe.Error, ClassificationLabel] = {
    decode[ClassificationLabel](n)
  }

  def decodeJsonSet(
      n: String
  ): Either[io.circe.Error, Set[ClassificationLabel]] = {
    decode[Set[ClassificationLabel]](n)
  }
}
