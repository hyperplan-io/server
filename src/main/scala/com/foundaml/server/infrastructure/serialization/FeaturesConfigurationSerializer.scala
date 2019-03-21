package com.foundaml.server.infrastructure.serialization

import com.foundaml.server.domain.models.{
  CustomFeaturesConfiguration,
  FeaturesConfiguration,
  StandardFeaturesConfiguration
}
import io.circe.parser.decode

/**
  * Created by Antoine Sauray on 20/03/2019.
  */
object FeaturesConfigurationSerializer {

  import cats.syntax.functor._
  import io.circe.{Decoder, Encoder}, io.circe.generic.auto._
  import io.circe.syntax._

  implicit val customFeaturesEncoder =
    CustomFeatureConfigurationSerializer.encoder
  implicit val customFeaturesDecoder =
    CustomFeatureConfigurationSerializer.decoder

  implicit val encoder: Encoder[FeaturesConfiguration] = Encoder.instance {
    case foo @ StandardFeaturesConfiguration(_, _, _) => foo.asJson
    case bar @ CustomFeaturesConfiguration(_) => bar.asJson
  }

  implicit val decoder: Decoder[FeaturesConfiguration] =
    List[Decoder[FeaturesConfiguration]](
      Decoder[StandardFeaturesConfiguration].widen,
      Decoder[CustomFeaturesConfiguration].widen
    ).reduceLeft(_ or _)

  def encodeJson(featuresConfiguration: FeaturesConfiguration): String = {
    featuresConfiguration.asJson.noSpaces
  }

  def decodeJson(n: String): Either[io.circe.Error, FeaturesConfiguration] = {
    decode[FeaturesConfiguration](n)
  }
}
