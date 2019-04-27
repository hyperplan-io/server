package com.foundaml.server.infrastructure.serialization

import com.foundaml.server.domain.models.{
  FeatureConfiguration,
  FeaturesConfiguration
}
import io.circe
import io.circe.{HCursor, Json}
import io.circe.parser._
import io.circe.syntax._

/**
  * Created by Antoine Sauray on 20/03/2019.
  */
object FeaturesConfigurationSerializer {

  import cats.syntax.functor._
  import io.circe.{Decoder, Encoder}
  import io.circe.syntax._

  implicit val customFeatureEncoder: Encoder[FeatureConfiguration] =
    (a: FeatureConfiguration) =>
      Json.obj(
        ("name", Json.fromString(a.name)),
        ("type", Json.fromString(a.featuresType)),
        ("description", Json.fromString(a.description))
      )

  implicit val customFeatureDecoder: Decoder[FeatureConfiguration] =
    (c: HCursor) =>
      for {
        name <- c.downField("name").as[String]
        featuresType <- c.downField("type").as[String]
        description <- c.downField("description").as[String]
      } yield {
        FeatureConfiguration(name, featuresType, description)
      }

  implicit val decoder: Decoder[FeaturesConfiguration] =
    (c: HCursor) => {
      c.as[List[FeatureConfiguration]](
          Decoder.decodeList[FeatureConfiguration](customFeatureDecoder)
        )
        .map(
          featureConfigurationList =>
            FeaturesConfiguration(featureConfigurationList)
        )
    }

  implicit val encoder: Encoder[FeaturesConfiguration] =
    (a: FeaturesConfiguration) =>
      Json.arr(a.configuration.map(customFeatureEncoder.apply): _*)

  def encodeJson(featuresConfiguration: FeaturesConfiguration): Json = {
    featuresConfiguration.asJson
  }
  def encodeJsonNoSpaces(
      featuresConfiguration: FeaturesConfiguration
  ): String = {
    featuresConfiguration.asJson.noSpaces
  }

  def decodeJson(n: String): Either[io.circe.Error, FeaturesConfiguration] = {
    decode[FeaturesConfiguration](n)
  }
}
