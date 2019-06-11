package com.hyperplan.infrastructure.serialization.events

import com.hyperplan.domain.models.events.{
  ClassificationPredictionEvent,
  PredictionEvent,
  RegressionPredictionEvent
}
import com.hyperplan.domain.models.features.Features.Features
import com.hyperplan.domain.models.labels.{ClassificationLabel, RegressionLabel}
import com.hyperplan.infrastructure.serialization.features.FeaturesSerializer

import io.circe.parser.decode
import io.circe.syntax._

object PredictionEventSerializer {

  import io.circe._

  implicit val featuresEncoder: Encoder[Features] =
    FeaturesSerializer.Implicits.encoder
  implicit val featuresDecoder: Decoder[Features] =
    FeaturesSerializer.Implicits.decoder

  implicit val classificationLabelEncoder: Encoder[ClassificationLabel] =
    (label: ClassificationLabel) =>
      Json.obj(
        ("label", Json.fromString(label.label)),
        ("probability", Json.fromFloatOrNull(label.probability))
      )

  implicit val classificationLabelDecoder: Decoder[ClassificationLabel] =
    (c: HCursor) =>
      for {
        label <- c.downField("label").as[String]
        probability <- c.downField("probability").as[Float]
      } yield ClassificationLabel(label, probability, "", "")

  implicit val regressionLabelEncoder: Encoder[RegressionLabel] =
    (label: RegressionLabel) =>
      Json.obj(
        ("label", Json.fromFloatOrNull(label.label))
      )
  implicit val regressionLabelDecoder: Decoder[RegressionLabel] =
    (c: HCursor) =>
      for {
        label <- c.downField("label").as[Float]
      } yield RegressionLabel(label, "")

  implicit val classificationLabelsEncoder: Encoder[Set[ClassificationLabel]] =
    Encoder.encodeSet[ClassificationLabel]

  implicit val classificationLabelsDecoder: Decoder[Set[ClassificationLabel]] =
    (c: HCursor) => Decoder.decodeSet[ClassificationLabel].apply(c)

  implicit val regressionLabelsEncoder: Encoder[Set[RegressionLabel]] =
    Encoder.encodeSet[RegressionLabel]

  implicit val regressionLabelsDecoder: Decoder[Set[RegressionLabel]] =
    (c: HCursor) => Decoder.decodeSet[RegressionLabel].apply(c)

  implicit val classificationPredictionEncoder
      : Encoder[ClassificationPredictionEvent] =
    (prediction: ClassificationPredictionEvent) =>
      Json.obj(
        ("type", Json.fromString(prediction.eventType)),
        ("id", Json.fromString(prediction.id)),
        ("predictionId", Json.fromString(prediction.predictionId)),
        ("projectId", Json.fromString(prediction.projectId)),
        ("algorithmId", Json.fromString(prediction.algorithmId)),
        ("features", featuresEncoder(prediction.features)),
        ("labels", classificationLabelsEncoder(prediction.labels)),
        ("example", Json.fromString(prediction.example))
      )

  implicit val regressionPredictionEncoder: Encoder[RegressionPredictionEvent] =
    (prediction: RegressionPredictionEvent) =>
      Json.obj(
        ("type", Json.fromString(prediction.eventType)),
        ("id", Json.fromString(prediction.id)),
        ("predictionId", Json.fromString(prediction.predictionId)),
        ("projectId", Json.fromString(prediction.projectId)),
        ("algorithmId", Json.fromString(prediction.algorithmId)),
        ("features", featuresEncoder(prediction.features)),
        ("labels", regressionLabelsEncoder(prediction.labels)),
        ("example", Json.fromFloatOrNull(prediction.example))
      )

  implicit val encoder: Encoder[PredictionEvent] = {
    case prediction: ClassificationPredictionEvent =>
      prediction.asJson
    case prediction: RegressionPredictionEvent =>
      prediction.asJson
  }

  implicit val decoder: Decoder[PredictionEvent] =
    (c: HCursor) =>
      c.downField("type")
        .as[String]
        .flatMap {
          case ClassificationPredictionEvent.eventType =>
            for {
              id <- c.downField("id").as[String]
              predictionId <- c.downField("predictionId").as[String]
              projectId <- c.downField("projectId").as[String]
              algorithmId <- c.downField("algorithmId").as[String]
              features <- c.downField("features").as[Features]
              labels <- c.downField("labels").as[Set[ClassificationLabel]]
              example <- c.downField("example").as[String]
            } yield
              ClassificationPredictionEvent(
                id,
                predictionId,
                projectId,
                algorithmId,
                features,
                labels,
                example
              )
          case RegressionPredictionEvent.eventType =>
            for {
              id <- c.downField("id").as[String]
              predictionId <- c.downField("predictionId").as[String]
              projectId <- c.downField("projectId").as[String]
              algorithmId <- c.downField("algorithmId").as[String]
              features <- c.downField("features").as[Features]
              labels <- c.downField("labels").as[Set[RegressionLabel]]
              example <- c.downField("example").as[Float]
            } yield
              RegressionPredictionEvent(
                id,
                predictionId,
                projectId,
                algorithmId,
                features,
                labels,
                example
              )
        }

  def encodeJson(request: PredictionEvent): Json = {
    request.asJson
  }

  def encodeJsonNoSpaces(request: PredictionEvent): String = {
    request.asJson.noSpaces
  }

  def decodeJson(n: String): PredictionEvent = {
    decode[PredictionEvent](n).right.get
  }
}
