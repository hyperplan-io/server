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

import io.circe._
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import com.hyperplan.domain.models.labels.{ClassificationLabel, Labels}
import com.hyperplan.domain.models.backends._
import com.hyperplan.domain.models.features.transformers._
import com.hyperplan.domain.models.labels.transformers._
import com.hyperplan.infrastructure.serialization.features.FeaturesTransformerSerializer
import com.hyperplan.infrastructure.serialization.labels.{
  ClassificationLabelSerializer,
  LabelsTransformerSerializer
}

object BackendSerializer {

  object Implicits {

    implicit val classificationLabelEncoder: Encoder[Set[ClassificationLabel]] =
      ClassificationLabelSerializer.classificationLabelsSetEncoder
    implicit val classificationLabelDecoder: Decoder[Set[ClassificationLabel]] =
      ClassificationLabelSerializer.classificationLabelsSetDecoder

    val localClassificationBackendEncoder: Encoder[LocalRandomClassification] =
      Encoder.forProduct2("class", "labels")(
        backend => (LocalRandomClassification.backendClass, backend.computed)
      )
    val localClassificationBackendDecoder: Decoder[LocalRandomClassification] =
      Decoder
        .forProduct2[LocalRandomClassification, String, Set[String]](
          "class",
          "labels"
        )((_, labels) => LocalRandomClassification(labels))

    val localRegressionBackendEncoder: Encoder[LocalRandomRegression] =
      Encoder.forProduct1("class")(
        _ => LocalRandomRegression.backendClass
      )

    val localRegressionBackendDecoder: Decoder[LocalRandomRegression] =
      Decoder
        .forProduct1[LocalRandomRegression, String](
          "class"
        )(_ => LocalRandomRegression())

    val tensorFlowClassificationBackendEncoder
        : Encoder[TensorFlowClassificationBackend] =
      (backend: TensorFlowClassificationBackend) =>
        Json.obj(
          (
            "class",
            Json.fromString(TensorFlowClassificationBackend.backendClass)
          ),
          ("rootPath", Json.fromString(backend.rootPath)),
          ("model", Json.fromString(backend.model)),
          (
            "modelVersion",
            backend.modelVersion.fold(Json.Null)(Json.fromString)
          ),
          (
            "featuresTransformer",
            tfTransformerEncoder.apply(backend.featuresTransformer)
          ),
          (
            "labelsTransformer",
            tfLabelsTransformerEncoder.apply(backend.labelsTransformer)
          )
        )

    val tensorFlowClassificationBackendDecoder
        : Decoder[TensorFlowClassificationBackend] =
      (c: HCursor) =>
        for {
          rootPath <- c.downField("rootPath").as[String]
          model <- c.downField("model").as[String]
          modelVersion <- c.downField("modelVersion").as[Option[String]]
          featuresTransformer <- c
            .downField("featuresTransformer")
            .as[TensorFlowFeaturesTransformer]
          labelsTransformer <- c
            .downField("labelsTransformer")
            .as[TensorFlowLabelsTransformer]
        } yield
          TensorFlowClassificationBackend(
            rootPath,
            model,
            modelVersion,
            featuresTransformer,
            labelsTransformer
          )

    val tensorFlowRegressionBackendEncoder
        : Encoder[TensorFlowRegressionBackend] =
      (backend: TensorFlowRegressionBackend) =>
        Json.obj(
          ("class", Json.fromString(TensorFlowRegressionBackend.backendClass)),
          ("rootPath", Json.fromString(backend.rootPath)),
          ("model", Json.fromString(backend.model)),
          (
            "modelVersion",
            backend.modelVersion.fold(Json.Null)(Json.fromString)
          ),
          (
            "featuresTransformer",
            tfTransformerEncoder.apply(backend.featuresTransformer)
          )
        )

    val tensorFlowRegressionBackendDecoder
        : Decoder[TensorFlowRegressionBackend] =
      (c: HCursor) =>
        for {
          rootPath <- c.downField("rootPath").as[String]
          model <- c.downField("model").as[String]
          modelVersion <- c.downField("modelVersion").as[Option[String]]
          featuresTransformer <- c
            .downField("featuresTransformer")
            .as[TensorFlowFeaturesTransformer]
        } yield
          TensorFlowRegressionBackend(
            rootPath,
            model,
            modelVersion,
            featuresTransformer
          )

    implicit val tfTransformerEncoder: Encoder[TensorFlowFeaturesTransformer] =
      FeaturesTransformerSerializer.tfTransformerEncoder
    implicit val ftTransformerDecoder: Decoder[TensorFlowFeaturesTransformer] =
      FeaturesTransformerSerializer.tfTransformerDecoder

    implicit val tfLabelsTransformerEncoder
        : Encoder[TensorFlowLabelsTransformer] =
      LabelsTransformerSerializer.tfLabelsTransformerEncoder
    implicit val tfLabelsTransformerDecoder
        : Decoder[TensorFlowLabelsTransformer] =
      LabelsTransformerSerializer.tfLabelsTransformerDecoder

    implicit val labelsEncoder: Encoder[Labels] = LabelsSerializer.encoder
    implicit val labelsDecoder: Decoder[Labels] = LabelsSerializer.decoder

    implicit val rasaNluFeaturesTransformerEncoder
        : Encoder[RasaNluFeaturesTransformer] =
      FeaturesTransformerSerializer.rasaNluTransformerEncoder
    implicit val rasaNluFeaturesTransformerDecoder
        : Decoder[RasaNluFeaturesTransformer] =
      FeaturesTransformerSerializer.rasaNluTransformerDecoder

    val rasaNluClassificationBackendEncoder
        : Encoder[RasaNluClassificationBackend] =
      (backend: RasaNluClassificationBackend) =>
        Json.obj(
          (
            "class",
            Json.fromString(RasaNluClassificationBackend.backendClass)
          ),
          ("rootPath", Json.fromString(backend.rootPath)),
          ("project", Json.fromString(backend.project)),
          ("model", Json.fromString(backend.model)),
          (
            "featuresTransformer",
            rasaNluFeaturesTransformerEncoder.apply(backend.featuresTransformer)
          )
        )

    val rasaNluClassificationBackendDecoder
        : Decoder[RasaNluClassificationBackend] =
      (c: HCursor) =>
        for {
          rootPath <- c.downField("rootPath").as[String]
          project <- c.downField("project").as[String]
          model <- c.downField("model").as[String]
          featuresTransformer <- c
            .downField("featuresTransformer")
            .as[RasaNluFeaturesTransformer]
        } yield
          RasaNluClassificationBackend(
            rootPath,
            project,
            model,
            featuresTransformer,
            RasaNluLabelsTransformer()
          )
  }

  import Implicits._

  implicit val decoder: Decoder[Backend] =
    (c: HCursor) =>
      c.downField("class").as[String].flatMap {
        case TensorFlowClassificationBackend.backendClass =>
          tensorFlowClassificationBackendDecoder(c)
        case TensorFlowRegressionBackend.backendClass =>
          tensorFlowRegressionBackendDecoder(c)
        case LocalRandomClassification.backendClass =>
          localClassificationBackendDecoder(c)
        case LocalRandomRegression.backendClass =>
          localRegressionBackendDecoder(c)
        case RasaNluClassificationBackend.backendClass =>
          rasaNluClassificationBackendDecoder(c)

      }

  implicit val encoder: Encoder[Backend] = {
    case backend: TensorFlowClassificationBackend =>
      tensorFlowClassificationBackendEncoder(backend)
    case backend: TensorFlowRegressionBackend =>
      tensorFlowRegressionBackendEncoder(backend)
    case backend: LocalRandomClassification =>
      localClassificationBackendEncoder(backend)
    case backend: LocalRandomRegression =>
      localRegressionBackendEncoder(backend)
    case backend: RasaNluClassificationBackend =>
      rasaNluClassificationBackendEncoder(backend)
  }

  def encodeJsonNoSpaces(backend: Backend): String = {
    backend.asJson.noSpaces
  }

  def encodeJson(backend: Backend): Json = {
    backend.asJson
  }

  def decodeJson(n: String): Either[io.circe.Error, Backend] = {
    decode[Backend](n)
  }
}
