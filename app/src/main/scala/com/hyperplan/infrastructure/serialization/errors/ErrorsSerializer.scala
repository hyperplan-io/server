package com.hyperplan.infrastructure.serialization.errors

import cats.effect.IO
import cats.implicits._
import cats.data._

import io.circe.{Decoder, Encoder, DecodingFailure, HCursor, Json}
import io.circe.generic.extras.Configuration
import io.circe.parser.decode
import io.circe.syntax._

import org.http4s.circe.jsonOf
import org.http4s.EntityDecoder

import com.hyperplan.domain.errors._

object ErrorsSerializer {

  def featureErrorToClass(error: FeaturesError): String = error match {
    case FeaturesDoesNotExistError(message) => "FeatureDoesNotExist"
    case UnsupportedDimensionError(message) => "UnsupportedDimension"
    case ReferenceFeatureDoesNotExistError(message) =>
      "ReferenceFeatureDoesNotExist"
    case RecursiveFeatureError(message) => "RecursivityError"
    case FeaturesAlreadyExistError(message) => "IdentifierAlreadyExist"
  }

  def classToFeatureError(
      errorClass: String,
      message: String
  ): Decoder.Result[FeaturesError] = errorClass match {
    case "FeatureDoesNotExist" =>
      FeaturesDoesNotExistError(message).asRight
    case "UnsupportedDimension" =>
      UnsupportedDimensionError(message).asRight
    case "ReferenceFeatureDoesNotExist" =>
      ReferenceFeatureDoesNotExistError(message).asRight
    case "RecursivityError" =>
      RecursiveFeatureError(message).asRight
    case "IdentifierAlreadyExist" =>
      FeaturesAlreadyExistError(message).asRight
    case _ => DecodingFailure("Unknown error class", Nil).asLeft
  }

  implicit val encoder: Encoder[NonEmptyChain[FeaturesError]] =
    (errors: NonEmptyChain[FeaturesError]) =>
      Json.obj(
        (
          "errors",
          Json.fromValues(
            errors.toNonEmptyList.map { error =>
              error.asJson
            }.toList
          )
        )
      )

  implicit val featureEncoder: Encoder[FeaturesError] =
    (error: FeaturesError) =>
      Json.obj(
        "class" -> Json.fromString(featureErrorToClass(error)),
        "message" -> Json.fromString(error.message)
      )

  implicit val featureDecoder: Decoder[FeaturesError] =
    (cursor: HCursor) =>
      for {
        errorClass <- cursor.downField("class").as[String]
        message <- cursor.downField("message").as[String]
        error <- classToFeatureError(errorClass, message)
      } yield error

  def encodeJson[E](errors: FeaturesError*): Json = {
    errors.asJson
  }

  implicit val featureErrorEntityDecoder
      : EntityDecoder[IO, List[FeaturesError]] =
    jsonOf[IO, List[FeaturesError]]
}
