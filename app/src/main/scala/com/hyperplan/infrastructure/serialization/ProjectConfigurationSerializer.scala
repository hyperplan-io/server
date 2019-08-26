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

import com.hyperplan.domain.models._
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import cats.syntax.functor._
import io.circe.{Decoder, Encoder}

object ProjectConfigurationSerializer {

  implicit val dataStreamEncoder: Encoder[StreamConfiguration] =
    (dataStream: StreamConfiguration) =>
      Json.obj(
        "topic" -> Json.fromString(dataStream.topic)
      )

  implicit val dataStreamDecoder: Decoder[StreamConfiguration] =
    (cursor: HCursor) =>
      for {
        topic <- cursor.downField("topic").as[String]
      } yield StreamConfiguration(topic)

  implicit val classificationConfigurationEncoder
      : Encoder[ClassificationConfiguration] =
    (configuration: ClassificationConfiguration) =>
      Json.obj(
        (
          "features",
          FeaturesConfigurationSerializer.encoder(configuration.features)
        ),
        (
          "labels",
          LabelsConfigurationSerializer.encodeJson(configuration.labels)
        ),
        (
          "dataStream",
          configuration.dataStream.fold(Json.Null)(_.asJson)
        )
      )

  implicit val classificationConfigurationDecoder
      : Decoder[ClassificationConfiguration] =
    (c: HCursor) =>
      for {
        featuresConfiguration <- c
          .downField("features")
          .as[FeatureVectorDescriptor](FeaturesConfigurationSerializer.decoder)
        labels <- c
          .downField("labels")
          .as[LabelVectorDescriptor](LabelsConfigurationSerializer.decoder)
        dataStream <- c
          .downField("dataStream")
          .as[Option[StreamConfiguration]]
      } yield {
        ClassificationConfiguration(
          featuresConfiguration,
          labels,
          dataStream
        )
      }

  implicit val regressionConfigurationEncoder
      : Encoder[RegressionConfiguration] =
    (configuration: RegressionConfiguration) =>
      Json.obj(
        (
          "features",
          FeaturesConfigurationSerializer.encoder(configuration.features)
        ),
        (
          "dataStream",
          configuration.dataStream.fold(Json.Null)(_.asJson)
        )
      )

  implicit val regressionConfigurationDecoder
      : Decoder[RegressionConfiguration] =
    (c: HCursor) =>
      for {
        featuresConfiguration <- c
          .downField("features")
          .as[FeatureVectorDescriptor](FeaturesConfigurationSerializer.decoder)
        dataStream <- c
          .downField("dataStream")
          .as[Option[StreamConfiguration]]
      } yield {
        RegressionConfiguration(featuresConfiguration, dataStream)
      }

  implicit val encoder: Encoder[ProjectConfiguration] = Encoder.instance {
    case config: ClassificationConfiguration => config.asJson
    case config: RegressionConfiguration => config.asJson
  }

  implicit val decoder: Decoder[ProjectConfiguration] =
    List[Decoder[ProjectConfiguration]](
      Decoder[ClassificationConfiguration].widen,
      Decoder[RegressionConfiguration].widen
    ).reduceLeft(_ or _)

  def encodeJson(project: ProjectConfiguration): Json = {
    project.asJson
  }

  def encodeJsonString(project: ProjectConfiguration): String = {
    project.asJson.noSpaces
  }

  def decodeJson(n: String): Either[io.circe.Error, ProjectConfiguration] = {
    decode[ProjectConfiguration](n)
  }

}
