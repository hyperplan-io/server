package com.foundaml.server.infrastructure.serialization

import io.circe.Encoder
import io.circe._
import io.circe.parser.decode
import io.circe.syntax._
import com.foundaml.server.domain.models.features._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveDecoder, deriveEncoder}

object FeaturesSerializer {

  object Implicits {
    implicit val discriminator: Configuration =
      Configuration.default.withDiscriminator("class")

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

  import io.circe.generic.extras.semiauto._
  import Implicits._

  implicit val encoder: Encoder[Features] = deriveEncoder
  implicit val decoder: Decoder[Features] = deriveDecoder


  def encodeJson(labels: Features): String = {
    labels.asJson.noSpaces
  }

  def decodeJson(n: String): Features = {
    decode[Features](n).right.get
  }

/*
  implicit val encodeTFClassificationFeatures
      : Encoder[TensorFlowClassificationFeatures] =
    (features: TensorFlowClassificationFeatures) =>
      Json.obj(
        ("signature_name", Json.fromString(features.signatureName)),
        ("examples", Json.arr(Json.fromFields(features.examples.flatMap {
          case TensorFlowDoubleFeature(key, value) =>
            Json.fromDouble(value).map(json => key -> json)
          case TensorFlowFloatFeature(key, value) =>
            Json.fromFloat(value).map(json => key -> json)
          case TensorFlowIntFeature(key, value) =>
            Some(key -> Json.fromInt(value))
          case TensorFlowStringFeature(key, value) =>
            Some(key -> Json.fromString(value))
        })))
      )

  def encodeJson(features: TensorFlowClassificationFeatures): Json =
    features.asJson
*/
}
