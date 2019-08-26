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

import com.hyperplan.domain.models.features.Features.Features
import com.hyperplan.domain.models.features._
import com.hyperplan.infrastructure.serialization.FeaturesConfigurationSerializer

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.parser.decode
import io.circe.syntax._
import org.http4s.circe.jsonOf
import io.circe.DecodingFailure

object FeaturesSerializer {

  object FeatureSerializer {

    import io.circe.generic.semiauto._

    implicit val floatEncoder: Encoder[FloatFeature] = deriveEncoder
    implicit val floatDecoder: Decoder[FloatFeature] = deriveDecoder

    implicit val intEncoder: Encoder[IntFeature] = deriveEncoder
    implicit val intDecoder: Decoder[IntFeature] = deriveDecoder

    implicit val stringEncoder: Encoder[StringFeature] = deriveEncoder
    implicit val stringDecoder: Decoder[StringFeature] = deriveDecoder

    implicit val intVectorEncoder: Encoder[IntArrayFeature] = deriveEncoder
    implicit val intVectorDecoder: Decoder[IntArrayFeature] = deriveDecoder

    implicit val floatVectorEncoder: Encoder[FloatArrayFeature] = deriveEncoder
    implicit val floatVectorDecoder: Decoder[FloatArrayFeature] = deriveDecoder

    implicit val stringVectorEncoder: Encoder[StringArrayFeature] =
      deriveEncoder
    implicit val stringVectorDecoder: Decoder[StringArrayFeature] =
      deriveDecoder

    implicit val featureTypeEncoder: Encoder[FeatureType] =
      FeaturesConfigurationSerializer.featureTypeEncoder
    implicit val featureTypeDecoder: Decoder[FeatureType] =
      FeaturesConfigurationSerializer.featureTypeDecoder

    implicit val featureDimensionEncoder: Encoder[FeatureDimension] =
      FeaturesConfigurationSerializer.featureDimensionEncoder

    implicit val featureDimensionDecoder: Decoder[FeatureDimension] =
      FeaturesConfigurationSerializer.featureDimensionDecoder

    implicit val featureEncoder: Encoder[Feature] = {
      case feature @ FloatFeature(key, value) =>
        Json.obj(
          "key" -> Json.fromString(key),
          "type" -> (feature.featureType: FeatureType).asJson,
          "dimension" -> (feature.dimension: FeatureDimension).asJson,
          "value" -> Json.fromDoubleOrNull(value)
        )

      case feature @ IntFeature(key, value) =>
        Json.obj(
          "key" -> Json.fromString(key),
          "type" -> (feature.featureType: FeatureType).asJson,
          "dimension" -> (feature.dimension: FeatureDimension).asJson,
          "value" -> Json.fromInt(value)
        )

      case feature @ StringFeature(key, value) =>
        Json.obj(
          "key" -> Json.fromString(key),
          "type" -> (feature.featureType: FeatureType).asJson,
          "dimension" -> (feature.dimension: FeatureDimension).asJson,
          "value" -> Json.fromString(value)
        )

      case feature @ FloatArrayFeature(key, values) =>
        Json.obj(
          "key" -> Json.fromString(key),
          "type" -> (feature.featureType: FeatureType).asJson,
          "dimension" -> (feature.dimension: FeatureDimension).asJson,
          "value" -> Json.fromValues(values.flatMap(Json.fromFloat))
        )

      case feature @ IntArrayFeature(key, values) =>
        Json.obj(
          "key" -> Json.fromString(key),
          "type" -> (feature.featureType: FeatureType).asJson,
          "dimension" -> (feature.dimension: FeatureDimension).asJson,
          "value" -> Json.fromValues(values.map(Json.fromInt))
        )

      case feature @ StringArrayFeature(key, values) =>
        Json.obj(
          "key" -> Json.fromString(key),
          "type" -> (feature.featureType: FeatureType).asJson,
          "dimension" -> (feature.dimension: FeatureDimension).asJson,
          "value" -> Json.fromValues(values.map(Json.fromString))
        )

      case feature @ FloatMatrixFeature(key, values2d) =>
        Json.obj(
          "key" -> Json.fromString(key),
          "type" -> (feature.featureType: FeatureType).asJson,
          "dimension" -> (feature.dimension: FeatureDimension).asJson,
          "value" -> Json.fromValues(values2d.map { v =>
            Json.fromValues(v.map(Json.fromFloatOrNull))
          })
        )

      case feature @ IntMatrixFeature(key, values2d) =>
        Json.obj(
          "key" -> Json.fromString(key),
          "type" -> (feature.featureType: FeatureType).asJson,
          "dimension" -> (feature.dimension: FeatureDimension).asJson,
          "value" -> Json.fromValues(values2d.map { v =>
            Json.fromValues(v.map(Json.fromInt))
          })
        )

      case feature @ StringMatrixFeature(key, values2d) =>
        Json.obj(
          "key" -> Json.fromString(key),
          "type" -> (feature.featureType: FeatureType).asJson,
          "dimension" -> (feature.dimension: FeatureDimension).asJson,
          "value" -> Json.fromValues(values2d.map { v =>
            Json.fromValues(v.map(Json.fromString))
          })
        )
      case feature @ ReferenceFeature(key, reference, value) =>
        Json.obj(
          "key" -> Json.fromString(key),
          "type" -> (feature.featureType: FeatureType).asJson,
          "dimension" -> (feature.dimension: FeatureDimension).asJson,
          "value" -> value.asJson
        )
    }

    implicit val featureDecoder: Decoder[Feature] = new Decoder[Feature] {
      import org.http4s.circe.DecodingFailures
      import cats.data.NonEmptyList
      def apply(c: HCursor): Decoder.Result[Feature] =
        for {
          featureKey <- c.downField("key").as[String]
          featureType <- c.downField("type").as[FeatureType]
          featureDimension <- c.downField("dimension").as[FeatureDimension]
          featureValue <- (featureType, featureDimension) match {
            case (FloatFeatureType, Scalar) =>
              c.downField("value").as[Float].map[Feature] { value =>
                FloatFeature(
                  featureKey,
                  value
                )
              }

            case (FloatFeatureType, Array) =>
              c.downField("value").as[List[Float]].map[Feature] { value =>
                FloatArrayFeature(
                  featureKey,
                  value
                )
              }

            case (FloatFeatureType, Matrix) =>
              c.downField("value").as[List[List[Float]]].map[Feature] { value =>
                FloatMatrixFeature(
                  featureKey,
                  value
                )
              }

            case (IntFeatureType, Scalar) =>
              c.downField("value").as[Int].map[Feature] { value =>
                IntFeature(
                  featureKey,
                  value
                )
              }
            case (IntFeatureType, Array) =>
              c.downField("value").as[List[Int]].map[Feature] { value =>
                IntArrayFeature(
                  featureKey,
                  value
                )
              }

            case (IntFeatureType, Matrix) =>
              c.downField("value").as[List[List[Int]]].map[Feature] { value =>
                IntMatrixFeature(
                  featureKey,
                  value
                )
              }

            case (StringFeatureType, Scalar) =>
              c.downField("value").as[String].map[Feature] { value =>
                StringFeature(
                  featureKey,
                  value
                )
              }

            case (StringFeatureType, Array) =>
              c.downField("value").as[List[String]].map[Feature] { value =>
                StringArrayFeature(
                  featureKey,
                  value
                )
              }

            case (StringFeatureType, Matrix) =>
              c.downField("value").as[List[List[String]]].map[Feature] {
                value =>
                  StringMatrixFeature(
                    featureKey,
                    value
                  )
              }

            case (ReferenceFeatureType(reference), Scalar) =>
              c.downField("value").as[List[Feature]].map[Feature] { value =>
                ReferenceFeature(
                  featureKey,
                  reference,
                  value
                )
              }

            case (feature, dimension) =>
              Left(
                DecodingFailure(
                  s"The feature $feature is unsupported with dimension $dimension",
                  Nil
                )
              )
          }
        } yield featureValue
    }

  }

  object Implicits {
    import FeatureSerializer._

    implicit val decoder: Decoder[List[Feature]] =
      Decoder.decodeList[Feature](featureDecoder)
    implicit val encoder: Encoder[List[Feature]] =
      Encoder.encodeList[Feature](featureEncoder)
  }

  import Implicits._

  def encodeJson(features: Features): Json = {
    features.asJson
  }

  def encodeJsonNoSpaces(features: Features): String = {
    features.asJson.noSpaces
  }

  def decodeJson(n: String): Either[io.circe.Error, Features] = {
    decode[Features](n)
  }

}
