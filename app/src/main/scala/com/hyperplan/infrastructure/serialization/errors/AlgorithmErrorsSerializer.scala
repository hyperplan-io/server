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

import com.hyperplan.domain.errors.AlgorithmError
import com.hyperplan.domain.errors.AlgorithmError._

object AlgorithmErrorsSerializer {

  def algorithmErrorToClass(error: AlgorithmError): String = error match {
    case ProjectDoesNotExistError(_) =>
      "ProjectDoesNotExistError"
    case WrongNumberOfFeaturesInTransformerError(_) =>
      "WrongNumberOfFeaturesInTransformerError"
    case WrongNumberOfLabelsInTransformerError(_) =>
      "WrongNumberOfLabelsInTransformerError"
    case IncompatibleFeaturesError(_) =>
      "IncompatibleFeaturesError"
    case IncompatibleLabelsError(_) =>
      "IncompatibleLabelsError"
    case AlgorithmAlreadyExistsError(_) =>
      "AlgorithmAlreadyExistsError"
    case IncompatibleAlgorithmError(_) =>
      "IncompatibleAlgorithmError"
    case AlgorithmDataIsIncorrectError(_) =>
      "AlgorithmDataIsIncorrectError"
    case UnsupportedProtocolError(_) =>
      "UnsupportedProtocolError"
    case AlgorithmIdIsNotAlphaNumerical(_) =>
      "AlgorithmIdIsNotAlphaNumerical"
  }

  def classToAlgorithmError(
      errorClass: String,
      message: String
  ): Decoder.Result[AlgorithmError] = errorClass match {
    case "ProjectDoesNotExistError" =>
      ProjectDoesNotExistError(message).asRight
    case "WrongNumberOfFeaturesInTransformerError" =>
      WrongNumberOfFeaturesInTransformerError(message).asRight
    case "WrongNumberOfLabelsInTransformerError" =>
      WrongNumberOfLabelsInTransformerError(message).asRight
    case "IncompatibleFeaturesError" =>
      IncompatibleFeaturesError(message).asRight
    case "IncompatibleLabelsError" =>
      IncompatibleLabelsError(message).asRight
    case "AlgorithmAlreadyExistsError" =>
      AlgorithmAlreadyExistsError(message).asRight
    case "IncompatibleAlgorithmError" =>
      IncompatibleAlgorithmError(message).asRight
    case "AlgorithmDataIsIncorrectError" =>
      AlgorithmDataIsIncorrectError(message).asRight
    case "UnsupportedProtocolError" =>
      UnsupportedProtocolError(message).asRight
    case "AlgorithmIdIsNotAlphaNumerical" =>
      AlgorithmIdIsNotAlphaNumerical(message).asRight
    case _ => DecodingFailure("Unknown error class", Nil).asLeft
  }

  implicit val algorithmsEncoder: Encoder[NonEmptyChain[AlgorithmError]] =
    (errors: NonEmptyChain[AlgorithmError]) =>
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

  implicit val algorithmEncoder: Encoder[AlgorithmError] =
    (error: AlgorithmError) =>
      Json.obj(
        "class" -> Json.fromString(algorithmErrorToClass(error)),
        "message" -> Json.fromString(error.message)
      )

  implicit val algorithmDecoder: Decoder[AlgorithmError] =
    (cursor: HCursor) =>
      for {
        errorClass <- cursor.downField("class").as[String]
        message <- cursor.downField("message").as[String]
        error <- classToAlgorithmError(errorClass, message)
      } yield error

  def encodeJson[E](errors: AlgorithmError*): Json = {
    errors.asJson
  }

  implicit val algorithmErrorEntityDecoder
      : EntityDecoder[IO, List[AlgorithmError]] =
    jsonOf[IO, List[AlgorithmError]]

}
