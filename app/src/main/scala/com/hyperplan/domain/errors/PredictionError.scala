package com.hyperplan.domain.errors

import com.hyperplan.domain.models.Project
import com.hyperplan.domain.models.backends.Backend
import com.hyperplan.domain.models.labels.ClassificationLabel

sealed trait PredictionError extends Exception {
  val message: String
  override def getMessage: String = message
}

object PredictionError {
  case class ProjectDoesNotExistError(message: String) extends PredictionError
  object ProjectDoesNotExistError {
    def message(projectId: String) = s"The project $projectId does not exist"
  }
  case class NoAlgorithmAvailableError() extends PredictionError {
    val message: String =
      "You did not set an algorithm id and the policy could not find an algorithm to execute"
  }
  case class AlgorithmDoesNotExistError(message: String) extends PredictionError
  object AlgorithmDoesNotExistError {
    def message(algorithmId: String) =
      s"The algorithm $algorithmId that you or the policy chose does not exist"
  }

  case class BackendExecutionError() extends PredictionError {
    val message: String =
      s"An error occurred with backend, please check the server logs"
  }

  case class FeaturesTransformerError() extends PredictionError {
    val message: String =
      "An error occurred with feature vector transformer, please check the server logs"
  }

  case class LabelsTransformerError() extends PredictionError {
    val message: String =
      "An error occurred with label vector transformer, please check the server logs"
  }

  case class CouldNotDecodeExamplesError(message: String)
      extends PredictionError
  object CouldNotDecodeExamplesError {
    def message(predictionId: String): String =
      s"Could not decode the examples for the prediction $predictionId"
  }

  case class CouldNotDecodeLabelsError(message: String) extends PredictionError
  object CouldNotDecodeLabelsError {
    def message(predictionId: String): String =
      s"Could not decode the labels for the prediction $predictionId"
  }

  case class PredictionAlreadyExistsError(message: String)
      extends PredictionError
  object PredictionAlreadyExistsError {
    def message(predictionId: String): String =
      s"The prediction $predictionId already exists and cannot be created"
  }

  case class PredictionDoesNotExistError(message: String)
      extends PredictionError
  object PredictionDoesNotExistError {
    def message(predictionId: String): String =
      s"The prediction $predictionId does not exist"
  }

  case class RegressionExampleShouldBeFloatError() extends PredictionError {
    val message: String = "A regression example should be a value of type float"
  }

  case class ClassificationExampleCannotBeEmptyError() extends PredictionError {
    val message: String = "A classification example cannot be an empty string"
  }

  case class ClassificationExampleDoesNotExist(message: String)
      extends PredictionError
  object ClassificationExampleDoesNotExist {
    def message(
        label: String,
        availableLabels: Set[ClassificationLabel]
    ): String =
      s"The label $label cannot be used as an example, it should be one of ${availableLabels.map(_.label).mkString(",")}"
  }

  case class IncompatibleBackendError(message: String) extends PredictionError
  object IncompatibleBackendError {
    def message(backend: Backend, project: Project): String =
      s"Project ${project.getClass.getSimpleName} is not compatible with backend ${backend.getClass.getSimpleName}"
  }

}
/*
case class NoAlgorithmAvailable(message: String)
    extends PredictionError(message)
case class FeaturesValidationFailed(message: String)
    extends PredictionError(message)
case class LabelsValidationFailed(message: String)
    extends PredictionError(message)

case class LabelNotFound(labelId: String)
    extends PredictionError(s"The label $labelId does not exist")

case class AlgorithmDoesNotExist(algorithmId: String)
    extends PredictionError(s"The algorithm $algorithmId does not exist")

case class BackendError(message: String) extends PredictionError(message)
case class PredictionAlreadyExist(predictionId: String)
    extends PredictionError(s"Prediction $predictionId already exists")

case class PredictionDataInconsistent(message: String)
    extends PredictionError(message)
case class CouldNotDecodeLabels(predictionId: String, expectedType: ProblemType)
    extends PredictionError(
      s"Could not decode the labels for the prediction $predictionId, expected type is $expectedType"
    )
case class CouldNotDecodeExamples(
    predictionId: String,
    expectedType: ProblemType
) extends PredictionError(
      s"Could not decode the examples for the prediction $predictionId, expected type is $expectedType"
    )
case class IncorrectExample(expectedType: ProblemType)
    extends PredictionError(
      s"this example is incorrect, expected type is $expectedType"
    )
 */
