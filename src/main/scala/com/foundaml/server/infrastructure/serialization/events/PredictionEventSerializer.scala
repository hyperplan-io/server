package com.foundaml.server.infrastructure.serialization.events

import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.events.{ClassificationPredictionEvent, PredictionEvent, RegressionPredictionEvent}
import com.foundaml.server.domain.models.features.Features.Features
import com.foundaml.server.domain.models.labels.{ClassificationLabel, RegressionLabel}
import com.foundaml.server.infrastructure.serialization.PredictionSerializer
import com.foundaml.server.infrastructure.serialization.features.FeaturesSerializer
import com.foundaml.server.infrastructure.serialization.labels.{ClassificationLabelSerializer, RegressionLabelSerializer}
import io.circe.parser.decode
import io.circe.syntax._

object PredictionEventSerializer {

  import io.circe._

  implicit val featuresEncoder: Encoder[Features] =
    FeaturesSerializer.Implicits.encoder
  implicit val featuresDecoder: Decoder[Features] =
    FeaturesSerializer.Implicits.decoder


  implicit val classificationLabelsEncoder: Encoder[Set[ClassificationLabel]] = PredictionSerializer.classificationLabelsEncoder

  implicit val classificationLabelsDecoder: Decoder[Set[ClassificationLabel]] = PredictionSerializer.classificationLabelsDecoder

  implicit val regressionLabelsEncoder: Encoder[Set[RegressionLabel]] = PredictionSerializer.regressionLabelsEncoder

  implicit val regressionLabelsDecoder: Decoder[Set[RegressionLabel]] = PredictionSerializer.regressionLabelsDecoder

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
