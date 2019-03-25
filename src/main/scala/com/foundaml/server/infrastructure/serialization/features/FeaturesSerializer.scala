package com.foundaml.server.infrastructure.serialization.features

import com.foundaml.server.domain.models.features.Features.Features
import com.foundaml.server.domain.models.features._
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.parser.decode
import io.circe.syntax._
import org.http4s.circe.jsonOf

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

    implicit val featureEncoder: Encoder[Feature] = {
      case FloatFeature(value) => Json.fromDoubleOrNull(value)
      case IntFeature(value) => Json.fromInt(value)
      case StringFeature(value) => Json.fromString(value)
      case FloatVectorFeature(values) =>
        Json.fromValues(values.flatMap(Json.fromFloat))
      case IntVectorFeature(values) => Json.fromValues(values.map(Json.fromInt))
      case StringVectorFeature(values) =>
        Json.fromValues(values.map(Json.fromString))
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
          jsonArr => {
            jsonArr.headOption.fold(
              Decoder.failedWithMessage[Feature]("features array is empty")(c)
            ) { headJson =>
              parseFeature(headJson.hcursor).fold(
                err =>
                  Decoder.failedWithMessage[Feature](
                    "features array could not be parsed"
                  )(c), {
                  case FloatFeature(values) =>
                    c.value
                      .as[List[Float]]
                      .map(values => FloatVectorFeature(values))
                  case IntFeature(values) =>
                    c.value
                      .as[List[Int]]
                      .map(values => IntVectorFeature(values))
                  case StringFeature(values) =>
                    c.value
                      .as[List[String]]
                      .map(values => StringVectorFeature(values))
                  case FloatVectorFeature(values) =>
                    c.value
                      .as[List[Float]]
                      .map(values => FloatVectorFeature(values))
                  case IntVectorFeature(values) =>
                    c.value
                      .as[List[Int]]
                      .map(values => IntVectorFeature(values))
                  case StringVectorFeature(values) =>
                    c.value
                      .as[List[String]]
                      .map(values => StringVectorFeature(values))
                }
              )
            }
          }
        )
      } else {
        parseFeature(c)
      }
    }

  }

  object Implicits {
    import FeatureSerializer._
    import io.circe.generic.extras.Configuration

    implicit val discriminator: Configuration =
      Configuration.default.withDiscriminator("class")

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
