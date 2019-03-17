package com.foundaml.server.infrastructure.serialization

import io.circe.Encoder
import io.circe._
import io.circe.parser.decode
import io.circe.syntax._
import com.foundaml.server.domain.models.features._

object FeaturesSerializer {

  object FeatureSerializer {
    import io.circe.generic.semiauto._

    implicit val doubleEncoder: Encoder[DoubleFeature] = deriveEncoder
    implicit val doubleDecoder: Decoder[DoubleFeature] = deriveDecoder

    implicit val floatEncoder: Encoder[FloatFeature] = deriveEncoder
    implicit val floatDecoder: Decoder[FloatFeature] = deriveDecoder

    implicit val intEncoder: Encoder[IntFeature] = deriveEncoder
    implicit val intDecoder: Decoder[IntFeature] = deriveDecoder

    implicit val stringEncoder: Encoder[StringFeature] = deriveEncoder
    implicit val stringDecoder: Decoder[StringFeature] = deriveDecoder

    implicit val featureEncoder: Encoder[Feature] = deriveEncoder
    implicit val featureDecoder: Decoder[Feature] = deriveDecoder
  }

  object Implicits {
    import io.circe.generic.extras.Configuration
    import io.circe.generic.extras.semiauto._

    import FeatureSerializer._

    implicit val discriminator: Configuration =
      Configuration.default.withDiscriminator("class")

    implicit val encoder: Encoder[Features] = deriveEncoder
    implicit val decoder: Decoder[Features] = deriveDecoder
  }

  import Implicits._

  def encodeJson(labels: Features): String = {
    labels.asJson.noSpaces
  }

  def decodeJson(n: String): Features = {
    decode[Features](n).right.get
  }

}
