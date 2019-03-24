package com.foundaml.server.domain.factories

import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.errors.{PredictionDataInconsistent, ProjectDataInconsistent}
import com.foundaml.server.domain.models.labels.{ClassificationLabel, RegressionLabel}
import com.foundaml.server.domain.repositories.{AlgorithmsRepository, PredictionsRepository, ProjectsRepository}
import scalaz.zio.{Task, ZIO}

class PredictionFactory(classificationsPredictionRepository: PredictionsRepository) {
  def get(predictionId: String): ZIO[Any, Throwable, Prediction] =
    classificationsPredictionRepository.read(predictionId).flatMap {
      case (id, projectId, algorithmId, Right(Classification()), Right(features), Right(labels), Right(examples)) =>
        val prediction = ClassificationPrediction(
          id,projectId,algorithmId, features, examples, labels.asInstanceOf[Set[ClassificationLabel]]
        )
        Task.succeed(prediction)
      case (id, projectId, algorithmId, Right(Regression()), Right(features), Right(labels), Right(examples)) =>
        val prediction = RegressionPrediction(
          id,projectId,algorithmId, features, examples, labels.asInstanceOf[Set[RegressionLabel]]
        )
        Task.succeed(prediction)
      case _ =>
        Task.fail(PredictionDataInconsistent(s"The prediction $predictionId cannot be restored"))
    }
}
