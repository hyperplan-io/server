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
    case DuplicateFeatureIds() => "DuplicateFeatureIds"
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
    case "DuplicateFeatureIds" =>
      DuplicateFeatureIds().asRight
    case "IdentifierAlreadyExist" =>
      FeaturesAlreadyExistError(message).asRight
    case _ => DecodingFailure("Unknown error class", Nil).asLeft
  }

  def labelErrorToClass(error: LabelsError): String = error match {
    case OneOfLabelsCannotBeEmpty() => "OneOfLabelsCannotBeEmpty"
    case LabelsAlreadyExist(_) => "LabelsAlreadyExist"
    case LabelsDoesNotExist(_) =>
      "LabelsDoesNotExist"
  }

  def classToLabelError(
      errorClass: String,
      message: String
  ): Decoder.Result[LabelsError] = errorClass match {
    case "OneOfLabelsCannotBeEmpty" =>
      OneOfLabelsCannotBeEmpty().asRight
    case "LabelsAlreadyExist" =>
      LabelsAlreadyExist(message).asRight
    case "LabelsDoesNotExist" =>
      LabelsDoesNotExist(message).asRight
  }

  implicit val featuresEncoder: Encoder[NonEmptyChain[FeaturesError]] =
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

  implicit val labelsEncoder: Encoder[NonEmptyChain[LabelsError]] =
    (errors: NonEmptyChain[LabelsError]) =>
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

  implicit val labelEncoder: Encoder[LabelsError] =
    (error: LabelsError) =>
      Json.obj(
        "class" -> Json.fromString(labelErrorToClass(error)),
        "message" -> Json.fromString(error.message)
      )

  implicit val labelDecoder: Decoder[LabelsError] =
    (cursor: HCursor) =>
      for {
        errorClass <- cursor.downField("class").as[String]
        message <- cursor.downField("message").as[String]
        error <- classToLabelError(errorClass, message)
      } yield error

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

  def encodeJsonLabels[E](errors: LabelsError*): Json = {
    errors.asJson
  }

  implicit val featureErrorEntityDecoder
      : EntityDecoder[IO, List[FeaturesError]] =
    jsonOf[IO, List[FeaturesError]]

  implicit val labelErrorEntityDecoder: EntityDecoder[IO, List[LabelsError]] =
    jsonOf[IO, List[LabelsError]]
}
