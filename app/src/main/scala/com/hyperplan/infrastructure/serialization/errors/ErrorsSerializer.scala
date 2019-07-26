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

  def featureErrorToClass(error: FeatureVectorDescriptorError): String = error match {
    case FeatureVectorDescriptorDoesNotExistError(message) => "FeatureDoesNotExist"
    case UnsupportedDimensionError(message) => "UnsupportedDimension"
    case ReferenceFeatureDoesNotExistError(message) =>
      "ReferenceFeatureDoesNotExist"
    case RecursiveFeatureError(message) => "RecursivityError"
    case FeatureVectorDescriptorAlreadyExistError(message) => "IdentifierAlreadyExist"
    case DuplicateFeatureIds() => "DuplicateFeatureIds"
  }

  def classToFeatureError(
      errorClass: String,
      message: String
  ): Decoder.Result[FeatureVectorDescriptorError] = errorClass match {
    case "FeatureDoesNotExist" =>
      FeatureVectorDescriptorDoesNotExistError(message).asRight
    case "UnsupportedDimension" =>
      UnsupportedDimensionError(message).asRight
    case "ReferenceFeatureDoesNotExist" =>
      ReferenceFeatureDoesNotExistError(message).asRight
    case "RecursivityError" =>
      RecursiveFeatureError(message).asRight
    case "DuplicateFeatureIds" =>
      DuplicateFeatureIds().asRight
    case "IdentifierAlreadyExist" =>
      FeatureVectorDescriptorAlreadyExistError(message).asRight
    case _ => DecodingFailure("Unknown error class", Nil).asLeft
  }

  def labelErrorToClass(error: LabelVectorDescriptorError): String = error match {
    case OneOfLabelVectorDescriptorCannotBeEmpty() => "OneOfLabelsCannotBeEmpty"
    case LabelVectorDescriptorAlreadyExist(_) => "LabelsAlreadyExist"
    case LabelVectorDescriptorDoesNotExist(_) =>
      "LabelsDoesNotExist"
  }

  def classToLabelError(
      errorClass: String,
      message: String
  ): Decoder.Result[LabelVectorDescriptorError] = errorClass match {
    case "OneOfLabelsCannotBeEmpty" =>
      OneOfLabelVectorDescriptorCannotBeEmpty().asRight
    case "LabelsAlreadyExist" =>
      LabelVectorDescriptorAlreadyExist(message).asRight
    case "LabelsDoesNotExist" =>
      LabelVectorDescriptorDoesNotExist(message).asRight
  }

  implicit val featuresEncoder: Encoder[NonEmptyChain[FeatureVectorDescriptorError]] =
    (errors: NonEmptyChain[FeatureVectorDescriptorError]) =>
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

  implicit val featureEncoder: Encoder[FeatureVectorDescriptorError] =
    (error: FeatureVectorDescriptorError) =>
      Json.obj(
        "class" -> Json.fromString(featureErrorToClass(error)),
        "message" -> Json.fromString(error.message)
      )

  implicit val labelsEncoder: Encoder[NonEmptyChain[LabelVectorDescriptorError]] =
    (errors: NonEmptyChain[LabelVectorDescriptorError]) =>
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

  implicit val labelEncoder: Encoder[LabelVectorDescriptorError] =
    (error: LabelVectorDescriptorError) =>
      Json.obj(
        "class" -> Json.fromString(labelErrorToClass(error)),
        "message" -> Json.fromString(error.message)
      )

  implicit val labelDecoder: Decoder[LabelVectorDescriptorError] =
    (cursor: HCursor) =>
      for {
        errorClass <- cursor.downField("class").as[String]
        message <- cursor.downField("message").as[String]
        error <- classToLabelError(errorClass, message)
      } yield error

  implicit val featureDecoder: Decoder[FeatureVectorDescriptorError] =
    (cursor: HCursor) =>
      for {
        errorClass <- cursor.downField("class").as[String]
        message <- cursor.downField("message").as[String]
        error <- classToFeatureError(errorClass, message)
      } yield error

  def encodeJson[E](errors: FeatureVectorDescriptorError*): Json = {
    errors.asJson
  }

  def encodeJsonLabels[E](errors: LabelVectorDescriptorError*): Json = {
    errors.asJson
  }

  implicit val featureErrorEntityDecoder
      : EntityDecoder[IO, List[FeatureVectorDescriptorError]] =
    jsonOf[IO, List[FeatureVectorDescriptorError]]

  implicit val labelErrorEntityDecoder: EntityDecoder[IO, List[LabelVectorDescriptorError]] =
    jsonOf[IO, List[LabelVectorDescriptorError]]
}
