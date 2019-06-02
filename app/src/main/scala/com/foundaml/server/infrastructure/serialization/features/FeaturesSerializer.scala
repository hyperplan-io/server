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
      case EmptyVectorFeature => Json.fromValues(Nil)
      case FloatVector2dFeature(values2d) =>
        Json.fromValues(values2d.map { v =>
          Json.fromValues(v.map(Json.fromFloatOrNull))
        })
      case IntVector2dFeature(values2d) =>
        Json.fromValues(values2d.map { v =>
          Json.fromValues(v.map(Json.fromInt))
        })
      case StringVector2dFeature(values2d) =>
        Json.fromValues(values2d.map { v =>
          Json.fromValues(v.map(Json.fromString))
        })
      case ReferenceFeature(value) =>
        ???

      case EmptyVector2dFeature => Json.fromValues(Nil)
    }

    import io.circe.DecodingFailure
    def computeType(value: Json): Either[DecodingFailure, Feature] =
      if (value.isNumber) {
        if (value.noSpaces.contains(".")) {
          value.as[Float].map(d => FloatFeature(d))
        } else {
          value.as[Int].map(d => IntFeature(d))
        }
      } else {
        value.as[String].map(s => StringFeature(s))
      }

    def parseFeature: Decoder[Feature] = (c: HCursor) => {
      if (c.value.isArray) {
        c.value.asArray.headOption.fold[Decoder.Result[Feature]](
          Right(EmptyVectorFeature)
        )(
          listHead =>
            listHead.headOption.fold[Decoder.Result[Feature]](
              Right(EmptyVectorFeature)
            ) { head =>
              computeType(head) match {
                case Right(StringFeature(_)) =>
                  c.value
                    .as[List[String]]
                    .map(values => StringVectorFeature(values))
                case Right(FloatFeature(_)) =>
                  c.value
                    .as[List[Float]]
                    .map(values => FloatVectorFeature(values))
                case Right(IntFeature(_)) =>
                  c.value.as[List[Int]].map(values => IntVectorFeature(values))
                case Right(_) =>
                  Decoder.failedWithMessage[Feature](
                    "Vectors of dimension > 2 are not supported"
                  )(c)
                case Left(_) =>
                  Decoder.failedWithMessage[Feature]("Unrecognized type")(c)

              }
            }
        )
      } else {
        computeType(c.value)
      }
    }

    implicit val featureDecoder: Decoder[Feature] = (c: HCursor) => {
      if (c.value.isArray) {
        c.value.asArray.fold[Decoder.Result[Feature]](
          Decoder.failedWithMessage[Feature]("features key is not an array")(c)
        )(
          jsonArr => {
            jsonArr.headOption.fold[Decoder.Result[Feature]] {
              Right(EmptyVectorFeature)
            } { headJson =>
              parseFeature(headJson.hcursor).fold(
                err => {
                  Decoder.failedWithMessage[Feature](
                    "features array could not be parsed"
                  )(c)
                }, {
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
                      .as[List[List[Float]]]
                      .map(values => FloatVector2dFeature(values))
                  case IntVectorFeature(values) =>
                    c.value
                      .as[List[List[Int]]]
                      .map(values => IntVector2dFeature(values))
                  case StringVectorFeature(values) =>
                    c.value
                      .as[List[List[String]]]
                      .map(values => StringVector2dFeature(values))
                  case EmptyVectorFeature =>
                    Right(EmptyVector2dFeature)
                  case _ =>
                    Decoder.failedWithMessage[Feature](
                      "Vectors of dimension > 2 are not supported"
                    )(c)
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
