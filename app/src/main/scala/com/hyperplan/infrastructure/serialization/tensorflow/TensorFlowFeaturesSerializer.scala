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

package com.hyperplan.infrastructure.serialization.tensorflow

import com.hyperplan.domain.models.features._
import io.circe.{Encoder, Json}
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf
import io.circe.syntax._
import cats.effect.IO
import cats.implicits._

object TensorFlowFeaturesSerializer {

  implicit val encodeTFClassificationFeatures: Encoder[TensorFlowFeatures] =
    (features: TensorFlowFeatures) =>
      Json.obj(
        ("signature_name", Json.fromString(features.signatureName)),
        ("examples", Json.arr(Json.fromFields(features.examples.flatMap {
          case TensorFlowFloatFeature(key, value) =>
            Json.fromDouble(value).map(json => key -> json)
          case TensorFlowIntFeature(key, value) =>
            Some(key -> Json.fromInt(value))
          case TensorFlowStringFeature(key, value) =>
            Some(key -> Json.fromString(value))
          case TensorFlowFloatVectorFeature(key, values) =>
            Some(key -> Json.fromValues(values.flatMap(Json.fromFloat)))
          case TensorFlowIntVectorFeature(key, values) =>
            Some(key -> Json.fromValues(values.map(Json.fromInt)))
          case TensorFlowStringVectorFeature(key, values) =>
            Some(key -> Json.fromValues(values.map(Json.fromString)))
          case TensorFlowEmptyVectorFeature(key) =>
            Some(key -> Json.fromValues(Nil))
          case TensorFlowIntVector2dFeature(key, values2d) =>
            Some(key -> Json.fromValues(values2d.map { v =>
              Json.fromValues(v.map(Json.fromInt))
            }))
          case TensorFlowFloatVector2dFeature(key, values2d) =>
            Some(key -> Json.fromValues(values2d.map { v =>
              Json.fromValues(v.map(Json.fromFloatOrNull))
            }))
          case TensorFlowStringVector2dFeature(key, values2d) =>
            Some(key -> Json.fromValues(values2d.map { v =>
              Json.fromValues(v.map(Json.fromString))
            }))

        })))
      )

  implicit val entityEncoder: EntityEncoder[IO, TensorFlowFeatures] =
    jsonEncoderOf[IO, TensorFlowFeatures]

  def encodeJson(features: TensorFlowFeatures): Json =
    features.asJson

}
