package com.foundaml.server.infrastructure.serialization

import com.foundaml.server.domain.models._
import io.circe.parser._
import io.circe.{HCursor, Json}

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

  implicit val encoder: Encoder[LabelsConfiguration] = {
    case oneOfConfig: OneOfLabelsConfiguration =>
      oneOfLabelsConfigEncoder(oneOfConfig)
    case dynamicConfig: DynamicLabelsConfiguration =>
      dynamicLabelsConfigEncoder(dynamicConfig)
  }

  implicit val decoder: Decoder[LabelsConfiguration] =
    (c: HCursor) =>
      c.downField("type").as[String].flatMap {
        case OneOfLabelsConfiguration.labelsType => oneOfLabelsConfigDecoder(c)
        case DynamicLabelsConfiguration.labelsType =>
          dynamicLabelsConfigDecoder(c)
      }

  def encodeJsonNoSpaces(featuresConfiguration: LabelsConfiguration): String = {
    featuresConfiguration.asJson.noSpaces
  }

  def encodeJson(featuresConfiguration: LabelsConfiguration): Json = {
    featuresConfiguration.asJson
  }

  def decodeJson(n: String): Either[io.circe.Error, LabelsConfiguration] = {
    decode[LabelsConfiguration](n)
  }
}
