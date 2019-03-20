package com.foundaml.server.infrastructure.serialization

import io.circe.Encoder
import io.circe._
import io.circe.parser.decode
import io.circe.syntax._
import com.foundaml.server.domain.models.features._

object FeaturesSerializer {

  object FeatureSerializer {
    import io.circe.generic.semiauto._

    implicit val floatEncoder: Encoder[DoubleFeature] = deriveEncoder
    implicit val floatDecoder: Decoder[DoubleFeature] = deriveDecoder

    implicit val intEncoder: Encoder[IntFeature] = deriveEncoder
    implicit val intDecoder: Decoder[IntFeature] = deriveDecoder

    implicit val stringEncoder: Encoder[StringFeature] = deriveEncoder
    implicit val stringDecoder: Decoder[StringFeature] = deriveDecoder

    implicit val featureEncoder: Encoder[CustomFeature] = {
      case DoubleFeature(value) => Json.fromDoubleOrNull(value)
      case IntFeature(value) => Json.fromInt(value)
      case StringFeature(value) => Json.fromString(value)
    }
    // encodeFoo: io.circe.Encoder[Thing] = $anon$1@50acf339

    implicit val featureDecoder: Decoder[CustomFeature] = new Decoder[CustomFeature] {
      final def apply(c: HCursor): Decoder.Result[CustomFeature] = {
        if(c.value.isNumber) {
          c.value.as[DoubleFeature]
        } else {
          c.value.as[StringFeature]
        }
      }
    }
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

  def encodeJson(labels: Features): Json = {
    labels.asJson
  }

  def encodeJsonNoSpaces(labels: Features): String = {
    labels.asJson.noSpaces
  }

  def decodeJson(n: String): Features = {
    decode[Features](n).right.get
  }

}
