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
    case ClassificationProjectRequiresLabels(message) => 
      "ClassificationProjectRequiresLabels"
    case ProjectDoesNotExist(projectId) =>
      "ProjectDoesNotExist"
    case FeaturesConfigurationError(message) =>
      "FeaturesConfigurationError"
    case ProjectAlreadyExists(projectId) =>
      "ProjectAlreadyExists"
    case InvalidProjectIdentifier(message) =>
      "InvalidProjectIdentifier"
    case ProjectDataInconsistent(projectId) =>
      "ProjectDataInconsistent"
    case RegressionProjectDoesNotRequireLabels(message) =>
      "RegressionProjectDoesNotRequireLabels"
    case FeaturesDoesNotExistError(message) => 
      "FeaturesDoesNotExistError"
    case LabelsDoesNotExistError(message) =>
      "LabelsDoesNotExistError"
    case ProjectIdIsNotAlphaNumerical(message) =>
      "ProjectIdIsNotAlphaNumerical"
    case ProjectLabelsAreRequiredForClassification() =>
      "ProjectLabelsAreRequiredForClassification"
  }

  def classToProjectError(
      errorClass: String,
      message: String
  ): Decoder.Result[ProjectError] = errorClass match {
    case "ClassificationProjectRequiresLabels" =>
      ClassificationProjectRequiresLabels(message).asRight
    case "ProjectDoesNotExist" =>
      ProjectDoesNotExist(message).asRight
    case "FeaturesConfigurationError" =>
      FeaturesConfigurationError(message).asRight
    case "ProjectAlreadyExists" =>
      ProjectAlreadyExists(message).asRight
    case "InvalidProjectIdentifier" =>
      InvalidProjectIdentifier(message).asRight
    case "ProjectDataInconsistent" =>
      ProjectDataInconsistent(message).asRight
    case "RegressionProjectDoesNotRequireLabels" =>
      RegressionProjectDoesNotRequireLabels(message).asRight
    case "FeaturesDoesNotExistError" =>
      FeaturesDoesNotExistError(message).asRight
    case "LabelsDoesNotExistError" =>
      LabelsDoesNotExistError(message).asRight
    case "ProjectIdIsNotAlphaNumerical" =>
      ProjectIdIsNotAlphaNumerical(message).asRight
    case "ProjectLabelsAreRequiredForClassification" =>
      ProjectLabelsAreRequiredForClassification().asRight
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
