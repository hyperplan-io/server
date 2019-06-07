package com.foundaml.server.infrastructure.serialization.features

import com.foundaml.server.domain.models.features.Features.Features
import com.foundaml.server.domain.models.features._
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.parser.decode
import io.circe.syntax._
import org.http4s.circe.jsonOf
import io.circe.DecodingFailure

object FeaturesSerializer {

  object FeatureSerializer {

    import io.circe.generic.semiauto._

    implicit val floatEncoder: Encoder[FloatFeature] = deriveEncoder
    implicit val floatDecoder: Decoder[FloatFeature] = deriveDecoder

    implicit val intEncoder: Encoder[IntFeature] = deriveEncoder
    implicit val intDecoder: Decoder[IntFeature] = deriveDecoder

    implicit val stringEncoder: Encoder[StringFeature] = deriveEncoder
    implicit val stringDecoder: Decoder[StringFeature] = deriveDecoder

    implicit val intVectorEncoder: Encoder[IntVectorFeature] = deriveEncoder
    implicit val intVectorDecoder: Decoder[IntVectorFeature] = deriveDecoder

    implicit val floatVectorEncoder: Encoder[FloatVectorFeature] = deriveEncoder
    implicit val floatVectorDecoder: Decoder[FloatVectorFeature] = deriveDecoder

    implicit val stringVectorEncoder: Encoder[StringVectorFeature] =
      deriveEncoder
    implicit val stringVectorDecoder: Decoder[StringVectorFeature] =
      deriveDecoder

    implicit val featureEncoder: Encoder[Feature] = deriveEncoder 

    implicit val featureDecoder: Decoder[Feature] = deriveDecoder 
  }

  object Implicits {
    import FeatureSerializer._

    implicit val decoder: Decoder[List[Feature]] =
      Decoder.decodeList[Feature](featureDecoder)
    implicit val encoder: Encoder[List[Feature]] =
      Encoder.encodeList[Feature](featureEncoder)
  }

  import Implicits._

  def encodeJson(features: Features): Json = {
    features.asJson
  }

  def encodeJsonNoSpaces(features: Features): String = {
    features.asJson.noSpaces
  }

  def decodeJson(n: String): Either[io.circe.Error, Features] = {
    decode[Features](n)
  }

}
