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

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.extras.Configuration
import io.circe.parser.decode
import io.circe.syntax._
import com.hyperplan.domain.models._

object AlgorithmPolicySerializer {

  object Implicits {
    implicit val discriminator: Configuration =
      Configuration.default.withDiscriminator("class")

    implicit val algorithmWeightDecoder: Decoder[AlgorithmWeight] =
      (c: HCursor) =>
        for {
          algorithmId <- c.downField("algorithmId").as[String]
          weight <- c.downField("weight").as[Float]
        } yield AlgorithmWeight(algorithmId, weight)

    implicit val algorithmWeightEncoder: Encoder[AlgorithmWeight] =
      (algorithmWeight: AlgorithmWeight) =>
        Json.obj(
          "algorithmId" -> Json.fromString(algorithmWeight.algorithmId),
          "weight" -> Json.fromFloatOrNull(algorithmWeight.weight)
        )

    implicit val encoder: Encoder[AlgorithmPolicy] = {
      case policy: NoAlgorithm =>
        Json.obj("class" -> Json.fromString(policy.name))
      case policy: DefaultAlgorithm =>
        Json.obj(
          "class" -> Json.fromString(policy.name),
          "algorithmId" -> Json.fromString(policy.algorithmId)
        )
      case policy: WeightedAlgorithm =>
        Json.obj(
          "class" -> Json.fromString(policy.name),
          "weights" -> Json.fromValues(policy.weights.map(_.asJson))
        )
    }

    implicit val decoder: Decoder[AlgorithmPolicy] =
      (c: HCursor) =>
        c.downField("class").as[String].flatMap {
          case NoAlgorithm.name => Right(NoAlgorithm())
          case DefaultAlgorithm.name =>
            c.downField("algorithmId").as[String].map { algorithmId =>
              DefaultAlgorithm(algorithmId)
            }
          case WeightedAlgorithm.name =>
            c.downField("weights").as[List[AlgorithmWeight]].map { weights =>
              WeightedAlgorithm(weights)
            }
        }
  }

  import Implicits._

  def encodeJsonString(policy: AlgorithmPolicy): String = {
    policy.asJson.noSpaces
  }

  def encodeJson(policy: AlgorithmPolicy): Json = {
    policy.asJson
  }

  def decodeJson(n: String): Either[io.circe.Error, AlgorithmPolicy] = {
    decode[AlgorithmPolicy](n)
  }
}
