package com.hyperplan.infrastructure.serialization

import io.circe._
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

import com.hyperplan.domain.models.features.transformers.TensorFlowFeaturesTransformer
import com.hyperplan.domain.models.labels.Labels
import com.hyperplan.domain.models.labels.transformers.TensorFlowLabelsTransformer
import com.hyperplan.infrastructure.serialization.features.FeaturesTransformerSerializer
import com.hyperplan.infrastructure.serialization.labels.LabelsTransformerSerializer
import com.hyperplan.domain.models.backends._

object BackendSerializer {

  object Implicits {

    val tensorFlowClassificationBackendEncoder
        : Encoder[TensorFlowClassificationBackend] =
      (backend: TensorFlowClassificationBackend) =>
        Json.obj(
          (
            "class",
            Json.fromString(TensorFlowClassificationBackend.backendClass)
          ),
          ("host", Json.fromString(backend.host)),
          ("port", Json.fromInt(backend.port)),
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
          host <- c.downField("host").as[String]
          port <- c.downField("port").as[Int]
          featuresTransformer <- c
            .downField("featuresTransformer")
            .as[TensorFlowFeaturesTransformer]
          labelsTransformer <- c
            .downField("labelsTransformer")
            .as[TensorFlowLabelsTransformer]
        } yield
          TensorFlowClassificationBackend(
            host,
            port,
            featuresTransformer,
            labelsTransformer
          )

    val tensorFlowRegressionBackendEncoder
        : Encoder[TensorFlowRegressionBackend] =
      (backend: TensorFlowRegressionBackend) =>
        Json.obj(
          ("class", Json.fromString(TensorFlowRegressionBackend.backendClass)),
          ("host", Json.fromString(backend.host)),
          ("port", Json.fromInt(backend.port)),
          (
            "featuresTransformer",
            tfTransformerEncoder.apply(backend.featuresTransformer)
          )
        )

    val tensorFlowRegressionBackendDecoder
        : Decoder[TensorFlowRegressionBackend] =
      (c: HCursor) =>
        for {
          host <- c.downField("host").as[String]
          port <- c.downField("port").as[Int]
          featuresTransformer <- c
            .downField("featuresTransformer")
            .as[TensorFlowFeaturesTransformer]
        } yield TensorFlowRegressionBackend(host, port, featuresTransformer)

    implicit val tfTransformerEncoder: Encoder[TensorFlowFeaturesTransformer] =
      FeaturesTransformerSerializer.tfTransformerEncoder
    implicit val ftTransformerDecoder: Decoder[TensorFlowFeaturesTransformer] =
      FeaturesTransformerSerializer.tfTransformerDecoder

    implicit val tfLabelsTransformerEncoder
        : Encoder[TensorFlowLabelsTransformer] =
      LabelsTransformerSerializer.tfLabelsTransformerEncoder
    implicit val labelsTransformerDecoder
        : Decoder[TensorFlowLabelsTransformer] =
      LabelsTransformerSerializer.labelsTransformerDecoder

    implicit val labelsEncoder: Encoder[Labels] = LabelsSerializer.encoder
    implicit val labelsDecoder: Decoder[Labels] = LabelsSerializer.decoder
  }

  import Implicits._

  implicit val decoder: Decoder[Backend] =
    (c: HCursor) =>
      c.downField("class").as[String].flatMap {
        case TensorFlowClassificationBackend.backendClass =>
          tensorFlowClassificationBackendDecoder(c)
        case TensorFlowRegressionBackend.backendClass =>
          tensorFlowRegressionBackendDecoder(c)
        case LocalClassification.backendClass => ???
        case RasaNluClassifcationBackend.backendClass => ???

      }

  implicit val encoder: Encoder[Backend] = {
    case backend: TensorFlowClassificationBackend =>
      tensorFlowClassificationBackendEncoder(backend)
    case backend: TensorFlowRegressionBackend =>
      tensorFlowRegressionBackendEncoder(backend)
    case backend: LocalClassification => ???
    case backend: RasaNluClassificationBackend => ???
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
