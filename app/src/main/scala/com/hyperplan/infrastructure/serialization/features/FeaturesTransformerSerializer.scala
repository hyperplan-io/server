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

package com.hyperplan.infrastructure.serialization.features

import com.hyperplan.domain.models.features.transformers.TensorFlowFeaturesTransformer
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.syntax._
import com.hyperplan.domain.models.features.transformers.RasaNluFeaturesTransformer

object FeaturesTransformerSerializer {

  val tfTransformerEncoder: Encoder[TensorFlowFeaturesTransformer] =
    (transformer: TensorFlowFeaturesTransformer) =>
      Json.obj(
        ("signatureName", Json.fromString(transformer.signatureName)),
        (
          "mapping",
          transformer.fields
            .map(keyValue => keyValue._1 -> Json.fromString(keyValue._2))
            .asJson
        )
      )

  val tfTransformerDecoder: Decoder[TensorFlowFeaturesTransformer] =
    (c: HCursor) =>
      for {
        signatureName <- c.downField("signatureName").as[String]
        fields <- c.downField("mapping").as[Map[String, String]]
      } yield TensorFlowFeaturesTransformer(signatureName, fields)

  val rasaNluTransformerEncoder: Encoder[RasaNluFeaturesTransformer] =
    (transformer: RasaNluFeaturesTransformer) =>
      Json.obj(
        ("field", Json.fromString(transformer.field)),
        ("joinCharacter", Json.fromString(transformer.joinCharacter))
      )

  val rasaNluTransformerDecoder: Decoder[RasaNluFeaturesTransformer] =
    (c: HCursor) =>
      for {
        field <- c.downField("field").as[String]
        joinCharacter <- c.downField("joinCharacter").as[String]
      } yield RasaNluFeaturesTransformer(field, joinCharacter)

}
