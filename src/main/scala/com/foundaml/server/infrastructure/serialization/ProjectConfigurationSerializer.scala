package com.foundaml.server.infrastructure.serialization

import com.foundaml.server.domain.models._
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import cats.syntax.functor._
import io.circe.{Decoder, Encoder}
object ProjectConfigurationSerializer {

  implicit val classificationConfigurationEncoder
      : Encoder[ClassificationConfiguration] =
    (a: ClassificationConfiguration) =>
      Json.obj(
        ("features", FeaturesConfigurationSerializer.encoder(a.features)),
        ("labels", Json.fromValues(a.labels.map(Json.fromString)))
      )

  implicit val classificationConfigurationDecoder
      : Decoder[ClassificationConfiguration] =
    (c: HCursor) =>
      for {
        featuresConfiguration <- c
          .downField("features")
          .as[FeaturesConfiguration](FeaturesConfigurationSerializer.decoder)
        labels <- c.downField("labels").as[Set[String]]
      } yield {
        ClassificationConfiguration(featuresConfiguration, labels)
      }

  implicit val regressionConfigurationEncoder
      : Encoder[RegressionConfiguration] =
    (a: RegressionConfiguration) =>
      Json.obj(
        ("features", FeaturesConfigurationSerializer.encoder(a.features))
      )

  implicit val regressionConfigurationDecoder
      : Decoder[RegressionConfiguration] =
    (c: HCursor) =>
      for {
        featuresConfiguration <- c
          .downField("features")
          .as[FeaturesConfiguration](FeaturesConfigurationSerializer.decoder)
      } yield {
        RegressionConfiguration(featuresConfiguration)
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
