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
