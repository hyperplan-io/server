package com.foundaml.server.infrastructure.serialization

import com.foundaml.server.domain.models.{CustomFeatureConfiguration, CustomFeaturesConfiguration, FeaturesConfiguration}
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

  implicit val customFeatureEncoder: Encoder[CustomFeatureConfiguration] =
    (a: CustomFeatureConfiguration) =>
      Json.obj(
        ("name", Json.fromString(a.name)),
        ("type", Json.fromString(a.featuresType)),
        ("description", Json.fromString(a.description))
      )

  implicit val customFeatureDecoder: Decoder[CustomFeatureConfiguration] =
    (c: HCursor) =>
      for {
        name <- c.downField("name").as[String]
        featuresType <- c.downField("type").as[String]
        description <- c.downField("description").as[String]
      } yield {
        CustomFeatureConfiguration(name, featuresType, description)
      }


  implicit val customFeaturesDecoder: Decoder[CustomFeaturesConfiguration] =
    (c: HCursor) => {
      c.as[List[CustomFeatureConfiguration]](Decoder.decodeList[CustomFeatureConfiguration](customFeatureDecoder))
        .map (featureConfigurationList => CustomFeaturesConfiguration(featureConfigurationList))
    }

  implicit val customFeaturesEncoder: Encoder[CustomFeaturesConfiguration] =
    (a: CustomFeaturesConfiguration) => Json.arr(a.featuresClasses.map(customFeatureEncoder.apply): _*)

  implicit val encoder: Encoder[FeaturesConfiguration] = Encoder.instance {
    case config @ CustomFeaturesConfiguration(_) => config.asJson(customFeaturesEncoder)
  }

  implicit val decoder: Decoder[FeaturesConfiguration] =
    List[Decoder[FeaturesConfiguration]](
      Decoder[CustomFeaturesConfiguration].widen
    ).reduceLeft(_ or _)

  def encodeJson(featuresConfiguration: FeaturesConfiguration): String = {
    featuresConfiguration.asJson.noSpaces
  }

  def decodeJson(n: String): Either[io.circe.Error, FeaturesConfiguration] = {
    decode[FeaturesConfiguration](n)
  }
}
