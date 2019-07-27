package com.hyperplan.infrastructure.serialization

import com.hyperplan.domain.models._
import io.circe.parser._
import io.circe.{HCursor, Json}

import cats.effect.IO
import org.http4s.circe.{jsonOf, jsonEncoderOf}
import org.http4s.{EntityDecoder, EntityEncoder}

/**
  * Created by Antoine Sauray on 20/03/2019.
  */
object LabelsConfigurationSerializer {

  import io.circe.syntax._
  import io.circe.{Decoder, Encoder}

  implicit val oneOfLabelsConfigEncoder: Encoder[OneOfLabelsDescriptor] = {
    oneOfConfig: OneOfLabelsDescriptor =>
      Json.obj(
        "type" -> Json.fromString(OneOfLabelsDescriptor.labelsType),
        "oneOf" -> Json.fromValues(oneOfConfig.oneOf.map(Json.fromString)),
        "description" -> Json.fromString(oneOfConfig.description)
      )
  }

  implicit val oneOfLabelsConfigDecoder: Decoder[OneOfLabelsDescriptor] =
    (c: HCursor) =>
      for {
        oneOf <- c.downField("oneOf").as[Set[String]]
        description <- c.downField("description").as[String]
      } yield OneOfLabelsDescriptor(oneOf, description)

  implicit val dynamicLabelsConfigEncoder: Encoder[DynamicLabelsDescriptor] = {
    dynamicConfig: DynamicLabelsDescriptor =>
      Json.obj(
        "type" -> Json.fromString(DynamicLabelsDescriptor.labelsType),
        "description" -> Json.fromString(dynamicConfig.description)
      )
  }

  implicit val dynamicLabelsConfigDecoder: Decoder[DynamicLabelsDescriptor] =
    (c: HCursor) =>
      for {
        description <- c.downField("description").as[String]
      } yield DynamicLabelsDescriptor(description)

  implicit val labelConfigurationDecoder: Decoder[LabelDescriptor] =
    (c: HCursor) =>
      c.downField("type").as[String].flatMap {
        case OneOfLabelsDescriptor.labelsType => oneOfLabelsConfigDecoder(c)
        case DynamicLabelsDescriptor.labelsType =>
          dynamicLabelsConfigDecoder(c)
      }

  implicit val labelConfigurationEncoder: Encoder[LabelDescriptor] =
    (labelConfiguration: LabelDescriptor) =>
      labelConfiguration match {
        case configuration: OneOfLabelsDescriptor =>
          oneOfLabelsConfigEncoder(configuration)
        case configuration: DynamicLabelsDescriptor =>
          dynamicLabelsConfigEncoder(configuration)
      }

  implicit val encoder: Encoder[LabelVectorDescriptor] = {
    case LabelVectorDescriptor(id, oneOfConfig: OneOfLabelsDescriptor) =>
      Json.obj(
        "id" -> Json.fromString(id),
        "data" -> oneOfLabelsConfigEncoder(oneOfConfig)
      )
    case LabelVectorDescriptor(id, dynamicConfig: DynamicLabelsDescriptor) =>
      Json.obj(
        "id" -> Json.fromString(id),
        "data" -> dynamicLabelsConfigEncoder(dynamicConfig)
      )
  }

  implicit val decoder: Decoder[LabelVectorDescriptor] =
    (c: HCursor) =>
      for {
        id <- c.downField("id").as[String]
        data <- c.downField("data").as[LabelDescriptor]
      } yield LabelVectorDescriptor(id, data)

  def encodeJsonNoSpaces(labelConfiguration: LabelVectorDescriptor): String = {
    labelConfiguration.asJson.noSpaces
  }

  def encodeJsonConfigurationNoSpaces(
      labelConfiguration: LabelDescriptor
  ): String = {
    labelConfiguration.asJson.noSpaces
  }

  def encodeJsonConfigurationListNoSpaces(
      labelConfiguration: List[LabelDescriptor]
  ): String = {
    labelConfiguration.asJson.noSpaces
  }

  def encodeJson(labelConfiguration: LabelVectorDescriptor): Json = {
    labelConfiguration.asJson
  }

  def encodeJsonList(labelConfiguration: List[LabelVectorDescriptor]): Json = {
    labelConfiguration.asJson
  }

  def decodeJson(n: String): Either[io.circe.Error, LabelVectorDescriptor] = {
    decode[LabelVectorDescriptor](n)
  }

  def decodeLabelConfigurationJson(
      n: String
  ): Either[io.circe.Error, LabelDescriptor] = {
    decode[LabelDescriptor](n)
  }

  def decodeLabelConfigurationListJson(
      n: String
  ): Either[io.circe.Error, List[LabelDescriptor]] = {
    decode[List[LabelDescriptor]](n)
  }

  implicit val entityDecoder: EntityDecoder[IO, LabelVectorDescriptor] =
    jsonOf[IO, LabelVectorDescriptor]

  implicit val entityEncoder: EntityEncoder[IO, LabelVectorDescriptor] =
    jsonEncoderOf[IO, LabelVectorDescriptor]

  implicit val entityDecoderList
      : EntityDecoder[IO, List[LabelVectorDescriptor]] =
    jsonOf[IO, List[LabelVectorDescriptor]]

}
