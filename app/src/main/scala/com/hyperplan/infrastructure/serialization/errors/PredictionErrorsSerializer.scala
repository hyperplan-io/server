package com.hyperplan.infrastructure.serialization.errors

import cats.data._
import cats.effect.IO
import cats.implicits._

import io.circe.syntax._
import io.circe._

import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import com.hyperplan.domain.errors.PredictionError
import com.hyperplan.domain.errors.PredictionError._

object PredictionErrorsSerializer {

  def predictionErrorToClass(error: PredictionError): String = error match {
    case ProjectDoesNotExistError(_) =>
      "ProjectDoesNotExistError"
    case NoAlgorithmAvailableError() =>
      "NoAlgorithmAvailableError"
    case AlgorithmDoesNotExistError(_) =>
      "AlgorithmDoesNotExistError"
    case FeaturesTransformerError() =>
      "FeaturesTransformerError"
    case LabelsTransformerError() =>
      "LabelsTransformerError"
    case BackendExecutionError(_) =>
      "BackendExecutionError"
    case CouldNotDecodeExamplesError(_) =>
      "CouldNotDecodeExamplesError"
    case CouldNotDecodeLabelsError(_) =>
      "CouldNotDecodeLabelsError"
    case PredictionAlreadyExistsError(_) =>
      "PredictionAlreadyExistsError"
    case PredictionDoesNotExistError(_) =>
      "PredictionDoesNotExistError"
    case RegressionExampleShouldBeFloatError() =>
      "RegressionExampleShouldBeFloatError"
    case ClassificationExampleCannotBeEmptyError() =>
      "ClassificationExampleCannotBeEmptyError"
    case ClassificationExampleDoesNotExist(_) =>
      "ClassificationExampleDoesNotExist"
    case IncompatibleBackendError(_) =>
      "IncompatibleBackendError"
  }

  def classToProjectError(
      errorClass: String,
      message: String
  ): Decoder.Result[PredictionError] = errorClass match {
    case "ProjectDoesNotExistError" =>
      ProjectDoesNotExistError(message).asRight
    case "NoAlgorithmAvailableError" =>
      NoAlgorithmAvailableError().asRight
    case "AlgorithmDoesNotExistError" =>
      AlgorithmDoesNotExistError(message).asRight
    case "FeaturesTransformerError" =>
      FeaturesTransformerError().asRight
    case "LabelsTransformerError" =>
      LabelsTransformerError().asRight
    case "BackendExecutionError" =>
      BackendExecutionError(message).asRight
    case "CouldNotDecodeExamplesError" =>
      CouldNotDecodeExamplesError(message).asRight
    case "CouldNotDecodeLabelsError" =>
      CouldNotDecodeLabelsError(message).asRight
    case "PredictionAlreadyExistsError" =>
      PredictionAlreadyExistsError(message).asRight
    case "PredictionDoesNotExistError" =>
      PredictionDoesNotExistError(message).asRight
    case "RegressionExampleShouldBeFloatError" =>
      RegressionExampleShouldBeFloatError().asRight
    case "ClassificationExampleCannotBeEmptyError" =>
      ClassificationExampleCannotBeEmptyError().asRight
    case "ClassificationExampleDoesNotExist" =>
      ClassificationExampleDoesNotExist(message).asRight
    case "IncompatibleBackendError" =>
      IncompatibleBackendError(message).asRight
    case _ => DecodingFailure("Unknown error class", Nil).asLeft
  }

  implicit val predictionChainEncoder: Encoder[NonEmptyChain[PredictionError]] =
    (errors: NonEmptyChain[PredictionError]) =>
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

  implicit val predictionsEncoder: Encoder[PredictionError] =
    (error: PredictionError) =>
      Json.obj(
        "class" -> Json.fromString(predictionErrorToClass(error)),
        "message" -> Json.fromString(error.message)
      )

  implicit val predictionsDecoder: Decoder[PredictionError] =
    (cursor: HCursor) =>
      for {
        errorClass <- cursor.downField("class").as[String]
        message <- cursor.downField("message").as[String]
        error <- classToProjectError(errorClass, message)
      } yield error

  def encodeJson[E](errors: PredictionError*): Json = {
    errors.asJson
  }

  implicit val predictionErrorsEntityDecoder
      : EntityDecoder[IO, List[PredictionError]] =
    jsonOf[IO, List[PredictionError]]

}
