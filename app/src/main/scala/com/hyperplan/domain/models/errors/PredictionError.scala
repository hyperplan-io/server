package com.hyperplan.domain.models.errors

import com.hyperplan.domain.models.ProblemType

class PredictionError(message: String) extends Throwable(message)

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
