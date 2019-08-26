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

import com.hyperplan.domain.models.labels.{ClassificationLabel, RegressionLabel}
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder, _}

object RegressionLabelSerializer {
  implicit val regressionlabelEncoder: Encoder[RegressionLabel] =
    (label: RegressionLabel) =>
      Json.obj(
        ("label", Json.fromFloatOrNull(label.label)),
        ("correctExampleUrl", Json.fromString(label.correctExampleUrl))
      )

  implicit val regressionLabelDecoder: Decoder[RegressionLabel] =
    (c: HCursor) =>
      for {
        label <- c.downField("label").as[Float]
        correctExampleUrl <- c.downField("correctExampleUrl").as[String]
      } yield RegressionLabel(label, correctExampleUrl)

  implicit val regressionLabelsSetEncoder: ArrayEncoder[Set[RegressionLabel]] =
    Encoder.encodeSet[RegressionLabel]
  implicit val regressionLabelsSetDecoder: Decoder[Set[RegressionLabel]] =
    Decoder.decodeSet[RegressionLabel]

  def encodeJsonNoSpaces(labels: RegressionLabel): String = {
    labels.asJson.noSpaces
  }

  def encodeJson(labels: RegressionLabel): Json = {
    labels.asJson
  }

  def encodeJsonSet(labels: Set[RegressionLabel]): Json = {
    labels.asJson
  }

  def encodeJsonSetNoSpaces(labels: Set[RegressionLabel]): String = {
    labels.asJson.noSpaces
  }

  def decodeJson(n: String): Either[io.circe.Error, RegressionLabel] = {
    decode[RegressionLabel](n)
  }

  def decodeJsonSet(n: String): Either[io.circe.Error, Set[RegressionLabel]] = {
    decode[Set[RegressionLabel]](n)
  }
}
