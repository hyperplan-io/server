package com.foundaml.server.infrastructure.serialization

import com.foundaml.server.domain.models.features.Features.Features
import io.circe.Encoder
import io.circe._
import io.circe.parser.decode
import io.circe.syntax._
import com.foundaml.server.domain.models.features._
import io.circe.Decoder.Result

object FeaturesSerializer {

  object FeatureSerializer {

    import io.circe.generic.semiauto._

    implicit val floatEncoder: Encoder[FloatFeature] = deriveEncoder
    implicit val floatDecoder: Decoder[FloatFeature] = deriveDecoder

    implicit val intEncoder: Encoder[IntFeature] = deriveEncoder
    implicit val intDecoder: Decoder[IntFeature] = deriveDecoder

    implicit val stringEncoder: Encoder[StringFeature] = deriveEncoder
    implicit val stringDecoder: Decoder[StringFeature] = deriveDecoder

    implicit val intVectorEncoder: Encoder[IntFeatures] = deriveEncoder
    implicit val intVectorDecoder: Decoder[IntFeatures] = deriveDecoder

    implicit val floatVectorEncoder: Encoder[FloatFeatures] = deriveEncoder
    implicit val floatVectorDecoder: Decoder[FloatFeatures] = deriveDecoder

    implicit val stringVectorEncoder: Encoder[StringFeatures] = deriveEncoder
    implicit val stringVectorDecoder: Decoder[StringFeatures] = deriveDecoder

    implicit val featureEncoder: Encoder[Feature] = {
      case FloatFeature(value) => Json.fromDoubleOrNull(value)
      case IntFeature(value) => Json.fromInt(value)
      case StringFeature(value) => Json.fromString(value)
    }

    def parseFeature: Decoder[Feature] = (c: HCursor) => {
      if (c.value.isNumber) {
        if (c.value.noSpaces.contains(".")) {
          c.value.as[Float].map(d => FloatFeature(d))
        } else {
          c.value.as[Int].map(d => IntFeature(d))
        }
      } else {
        c.value.as[String].map(s => StringFeature(s))
      }
    }

    implicit val featureDecoder: Decoder[Feature] = (c: HCursor) => {
      if (c.value.isArray) {
        c.value.asArray.fold[Decoder.Result[Feature]](
          Decoder.failedWithMessage[Feature]("features key is not an array")(c)
        )(
          jsonArr =>
            jsonArr.headOption.fold[Decoder.Result[Feature]](
              Decoder.failedWithMessage[Feature]("features array is empty")(c)
            )(
              head => parseFeature(head.hcursor)
            )
        )
      } else {
        parseFeature(c)
      }

    }

  }

  object Implicits {
    import io.circe.generic.extras.Configuration
    import io.circe.generic.extras.semiauto._

    import FeatureSerializer._

    implicit val discriminator: Configuration =
      Configuration.default.withDiscriminator("class")

    implicit val decoder: Decoder[List[Feature]] =
      Decoder.decodeList[Feature](featureDecoder)
    implicit val encoder: Encoder[List[Feature]] =
      Encoder.encodeList[Feature](featureEncoder)
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
