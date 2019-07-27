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

import com.hyperplan.domain.errors.ProjectError
import com.hyperplan.domain.errors.ProjectError._

object ProjectErrorsSerializer {

  def projectErrorToClass(error: ProjectError): String = error match {
    case ClassificationProjectRequiresLabelsError(_) =>
      "ClassificationProjectRequiresLabels"
    case ProjectDoesNotExistError(_) =>
      "ProjectDoesNotExist"
    case ProjectIdIsEmptyError() =>
      "ProjectIdIsEmptyError"
    case ProjectNameIsEmptyError() =>
      "ProjectNameIsEmptyError"
    case ProjectPolicyAlgorithmDoesNotExist(_) =>
      "ProjectPolicyAlgorithmDoesNotExist"
    case FeaturesConfigurationError(_) =>
      "FeaturesConfigurationError"
    case ProjectAlreadyExistsError(_) =>
      "ProjectAlreadyExists"
    case InvalidProjectIdentifierError(_) =>
      "InvalidProjectIdentifier"
    case ProjectDataInconsistentError(_) =>
      "ProjectDataInconsistent"
    case RegressionProjectDoesNotRequireLabelsError(_) =>
      "RegressionProjectDoesNotRequireLabels"
    case FeaturesDoesNotExistError(_) =>
      "FeaturesDoesNotExistError"
    case LabelsDoesNotExistError(_) =>
      "LabelsDoesNotExistError"
    case ProjectIdIsNotAlphaNumericalError(_) =>
      "ProjectIdIsNotAlphaNumerical"
    case ProjectLabelsAreRequiredForClassificationError() =>
      "ProjectLabelsAreRequiredForClassification"
    case AlgorithmDataIsIncorrectError(_) =>
      "AlgorithmDataIsIncorrectError"
  }

  def classToProjectError(
      errorClass: String,
      message: String
  ): Decoder.Result[ProjectError] = errorClass match {
    case "ClassificationProjectRequiresLabels" =>
      ClassificationProjectRequiresLabelsError(message).asRight
    case "ProjectDoesNotExist" =>
      ProjectDoesNotExistError(message).asRight
    case "ProjectIdIsEmptyError" =>
      ProjectIdIsEmptyError().asRight
    case "ProjectNameIsEmptyError" =>
      ProjectNameIsEmptyError().asRight
    case "FeaturesConfigurationError" =>
      FeaturesConfigurationError(message).asRight
    case "ProjectPolicyAlgorithmDoesNotExist" =>
      ProjectPolicyAlgorithmDoesNotExist(message).asRight
    case "ProjectAlreadyExists" =>
      ProjectAlreadyExistsError(message).asRight
    case "InvalidProjectIdentifier" =>
      InvalidProjectIdentifierError(message).asRight
    case "ProjectDataInconsistent" =>
      ProjectDataInconsistentError(message).asRight
    case "RegressionProjectDoesNotRequireLabels" =>
      RegressionProjectDoesNotRequireLabelsError(message).asRight
    case "FeaturesDoesNotExistError" =>
      FeaturesDoesNotExistError(message).asRight
    case "LabelsDoesNotExistError" =>
      LabelsDoesNotExistError(message).asRight
    case "ProjectIdIsNotAlphaNumerical" =>
      ProjectIdIsNotAlphaNumericalError(message).asRight
    case "ProjectLabelsAreRequiredForClassification" =>
      ProjectLabelsAreRequiredForClassificationError().asRight
    case "AlgorithmDataIsIncorrectError" =>
      AlgorithmDataIsIncorrectError(message).asRight
    case _ => DecodingFailure("Unknown error class", Nil).asLeft
  }

  implicit val projectsEncoder: Encoder[NonEmptyChain[ProjectError]] =
    (errors: NonEmptyChain[ProjectError]) =>
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

  implicit val projectEncoder: Encoder[ProjectError] =
    (error: ProjectError) =>
      Json.obj(
        "class" -> Json.fromString(projectErrorToClass(error)),
        "message" -> Json.fromString(error.message)
      )

  implicit val projectDecoder: Decoder[ProjectError] =
    (cursor: HCursor) =>
      for {
        errorClass <- cursor.downField("class").as[String]
        message <- cursor.downField("message").as[String]
        error <- classToProjectError(errorClass, message)
      } yield error

  def encodeJson[E](errors: ProjectError*): Json = {
    errors.asJson
  }

  implicit val projectErrorEntityDecoder
      : EntityDecoder[IO, List[ProjectError]] =
    jsonOf[IO, List[ProjectError]]

}
