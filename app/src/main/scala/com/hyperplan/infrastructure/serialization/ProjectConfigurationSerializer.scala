package com.hyperplan.infrastructure.serialization

import com.hyperplan.domain.models._
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import cats.syntax.functor._
import io.circe.{Decoder, Encoder}

object ProjectConfigurationSerializer {

  implicit val streamConfigurationEncoder: Encoder[StreamConfiguration] =
    (streamConfiguration: StreamConfiguration) =>
      Json.obj(
        "topic" -> Json.fromString(streamConfiguration.topic)
      )

  implicit val streamConfigurationDecoder: Decoder[StreamConfiguration] =
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
          "streamConfiguration",
          configuration.dataStream.fold(Json.Null)(_.asJson)
        )
      )

  implicit val classificationConfigurationDecoder
      : Decoder[ClassificationConfiguration] =
    (c: HCursor) =>
      for {
        featuresConfiguration <- c
          .downField("features")
          .as[FeaturesConfiguration](FeaturesConfigurationSerializer.decoder)
        labels <- c
          .downField("labels")
          .as[LabelsConfiguration](LabelsConfigurationSerializer.decoder)
        streamConfiguration <- c
          .downField("streamConfiguration")
          .as[Option[StreamConfiguration]]
      } yield {
        ClassificationConfiguration(
          featuresConfiguration,
          labels,
          streamConfiguration
        )
      }

  implicit val regressionConfigurationEncoder
      : Encoder[RegressionConfiguration] =
    (configuration: RegressionConfiguration) =>
      Json.obj(
        (
          "features",
          FeaturesConfigurationSerializer.encoder(configuration.features)
        )
      )

  implicit val regressionConfigurationDecoder
      : Decoder[RegressionConfiguration] =
    (c: HCursor) =>
      for {
        featuresConfiguration <- c
          .downField("features")
          .as[FeaturesConfiguration](FeaturesConfigurationSerializer.decoder)
        streamConfiguration <- c
          .downField("streamConfiguration")
          .as[Option[StreamConfiguration]]
      } yield {
        RegressionConfiguration(featuresConfiguration, streamConfiguration)
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
