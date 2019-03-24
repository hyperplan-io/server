package com.foundaml.server.domain.factories

import com.foundaml.server.domain.models
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.errors.{
  CouldNotDecodeExamples,
  CouldNotDecodeLabels,
  PredictionDataInconsistent
}
import com.foundaml.server.domain.repositories.PredictionsRepository
import com.foundaml.server.infrastructure.logging.IOLogging
import com.foundaml.server.infrastructure.serialization.examples.{
  ClassificationExamplesSerializer,
  RegressionExamplesSerializer
}
import com.foundaml.server.infrastructure.serialization.labels.{
  ClassificationLabelSerializer,
  RegressionLabelSerializer
}
import scalaz.zio.{Task, ZIO}

class PredictionFactory(
    classificationsPredictionRepository: PredictionsRepository
) extends IOLogging {
  def get(predictionId: String): ZIO[Any, Throwable, Prediction] =
    classificationsPredictionRepository.read(predictionId).flatMap {
      case (
          id,
          projectId,
          algorithmId,
          Right(Classification()),
          Right(features),
          labelsRaw,
          examplesRaw
          ) =>
        ClassificationLabelSerializer
          .decodeJsonSet(labelsRaw)
          .fold(
            err =>
              warnLog(err.getMessage) *> Task
                .fail(CouldNotDecodeLabels(predictionId, Classification())),
            classificationLabels => {
              ClassificationExamplesSerializer
                .decodeJson(examplesRaw)
                .fold(
                  err =>
                    warnLog(err.getMessage) *> Task.fail(
                      CouldNotDecodeExamples(predictionId, Classification())
                    ),
                  classificationExamples => {
                    val prediction = ClassificationPrediction(
                      id,
                      projectId,
                      algorithmId,
                      features,
                      classificationExamples,
                      classificationLabels
                    )
                    Task.succeed(prediction)
                  }
                )
            }
          )

      case (
          id,
          projectId,
          algorithmId,
          Right(Regression()),
          Right(features),
          labels,
          examplesRaw
          ) =>
        RegressionLabelSerializer
          .decodeJsonSet(labels)
          .fold(
            err =>
              warnLog(err.getMessage) *> Task
                .fail(CouldNotDecodeLabels(predictionId, Regression())),
            classificationLabels => {
              RegressionExamplesSerializer
                .decodeJson(examplesRaw)
                .fold(
                  err =>
                    warnLog(err.getMessage) *> Task.fail(
                      CouldNotDecodeExamples(predictionId, Classification())
                    ),
                  regressionExamples => {
                    val prediction = RegressionPrediction(
                      id,
                      projectId,
                      algorithmId,
                      features,
                      regressionExamples,
                      classificationLabels
                    )
                    Task.succeed(prediction)
                  }
                )
            }
          )
      case predictionData =>
        warnLog(s"Prediction data is inconsistent: $predictionData") *> Task
          .fail(
            PredictionDataInconsistent(
              s"The prediction $predictionId cannot be restored"
            )
          )
    }
}
