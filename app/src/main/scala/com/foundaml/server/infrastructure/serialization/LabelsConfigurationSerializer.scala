package com.foundaml.server.infrastructure.serialization

import com.foundaml.server.domain.models._
import io.circe.parser._
import io.circe.{HCursor, Json}

import cats.effect.IO
import org.http4s.circe.jsonOf
import org.http4s.EntityDecoder

/**
  * Created by Antoine Sauray on 20/03/2019.
  */
object LabelsConfigurationSerializer {

  import io.circe.syntax._
  import io.circe.{Decoder, Encoder}

  implicit val oneOfLabelsConfigEncoder: Encoder[OneOfLabelsConfiguration] = {
    oneOfConfig: OneOfLabelsConfiguration =>
      Json.obj(
        "type" -> Json.fromString(OneOfLabelsConfiguration.labelsType),
        "oneOf" -> Json.fromValues(oneOfConfig.oneOf.map(Json.fromString)),
        "description" -> Json.fromString(oneOfConfig.description)
      )
  }

  implicit val oneOfLabelsConfigDecoder: Decoder[OneOfLabelsConfiguration] =
    (c: HCursor) =>
      for {
        oneOf <- c.downField("oneOf").as[Set[String]]
        description <- c.downField("description").as[String]
      } yield OneOfLabelsConfiguration(oneOf, description)

  implicit val dynamicLabelsConfigEncoder
      : Encoder[DynamicLabelsConfiguration] = {
    dynamicConfig: DynamicLabelsConfiguration =>
      Json.obj(
        "type" -> Json.fromString(DynamicLabelsConfiguration.labelsType),
        "description" -> Json.fromString(dynamicConfig.description)
      )
  }

  implicit val dynamicLabelsConfigDecoder: Decoder[DynamicLabelsConfiguration] =
    (c: HCursor) =>
      for {
        description <- c.downField("description").as[String]
      } yield DynamicLabelsConfiguration(description)

  implicit val labelConfigurationDecoder: Decoder[LabelConfiguration] =
    (c: HCursor) =>
      c.downField("type").as[String].flatMap {
        case OneOfLabelsConfiguration.labelsType => oneOfLabelsConfigDecoder(c)
        case DynamicLabelsConfiguration.labelsType =>
          dynamicLabelsConfigDecoder(c)
      }

  implicit val labelConfigurationEncoder: Encoder[LabelConfiguration] =
    (labelConfiguration: LabelConfiguration) =>
      labelConfiguration match {
        case configuration: OneOfLabelsConfiguration =>
          oneOfLabelsConfigEncoder(configuration)
        case configuration: DynamicLabelsConfiguration =>
          dynamicLabelsConfigEncoder(configuration)
      }

  implicit val encoder: Encoder[LabelsConfiguration] = {
    case LabelsConfiguration(id, oneOfConfig: OneOfLabelsConfiguration) =>
      Json.obj(
        "id" -> Json.fromString(id),
        "data" -> oneOfLabelsConfigEncoder(oneOfConfig)
      )
    case LabelsConfiguration(id, dynamicConfig: DynamicLabelsConfiguration) =>
      Json.obj(
        "id" -> Json.fromString(id),
        "data" -> dynamicLabelsConfigEncoder(dynamicConfig)
      )
  }

  implicit val decoder: Decoder[LabelsConfiguration] =
    (c: HCursor) =>
      for {
        id <- c.downField("id").as[String]
        data <- c.downField("data").as[LabelConfiguration]
      } yield LabelsConfiguration(id, data)

  def encodeJsonNoSpaces(labelConfiguration: LabelsConfiguration): String = {
    labelConfiguration.asJson.noSpaces
  }

  def encodeJsonConfigurationNoSpaces(
      labelConfiguration: LabelConfiguration
  ): String = {
    labelConfiguration.asJson.noSpaces
  }

  def encodeJsonConfigurationListNoSpaces(
      labelConfiguration: List[LabelConfiguration]
  ): String = {
    labelConfiguration.asJson.noSpaces
  }

  def encodeJson(labelConfiguration: LabelsConfiguration): Json = {
    labelConfiguration.asJson
  }

  def decodeJson(n: String): Either[io.circe.Error, LabelsConfiguration] = {
    decode[LabelsConfiguration](n)
  }

  def decodeLabelConfigurationJson(
      n: String
  ): Either[io.circe.Error, LabelConfiguration] = {
    decode[LabelConfiguration](n)
  }

  def decodeLabelConfigurationListJson(
      n: String
  ): Either[io.circe.Error, List[LabelConfiguration]] = {
    decode[List[LabelConfiguration]](n)
  }

  implicit val entityDecoder: EntityDecoder[IO, LabelsConfiguration] =
    jsonOf[IO, LabelsConfiguration]

}
