package com.foundaml.server.application.controllers

import org.http4s.{HttpService, _}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe._
import scalaz.zio.Task
import scalaz.zio.interop.catz._
import com.foundaml.server.application.controllers.requests._
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.backends._
import com.foundaml.server.domain.models.errors.PredictionError
import com.foundaml.server.domain.models.features.DoubleFeatures
import com.foundaml.server.domain.models.labels._
import com.foundaml.server.domain.repositories._
import com.foundaml.server.domain.services.PredictionsService
import com.foundaml.server.infrastructure.serialization.{
  LabelsSerializer,
  PredictionRequestEntitySerializer
}

class PredictionsHttpService(
    predictionsService: PredictionsService,
    projectsRepository: ProjectsRepository,
    algorithmsRepository: AlgorithmsRepository
) extends Http4sDsl[Task] {

  val service: HttpService[Task] = {
    HttpService[Task] {
      case req @ POST -> Root =>
        (for {
          predictionRequest <- req
            .attemptAs[PredictionRequest](
              PredictionRequestEntitySerializer.requestDecoder
            )
            .fold(throw _, identity)
          labels <- predict(predictionRequest)
        } yield labels).flatMap { labels =>
          labels.fold(
            _ =>
              InternalServerError(
                "An error occurred while computing this prediction, check your logs"
              ),
            labels => Ok(LabelsSerializer.encodeJson(labels))
          )
        }
    }
  }

  def predict(
      request: PredictionRequest
  ): Task[Either[PredictionError, Labels]] = {

    val computed = Labels(
      Set(
        ClassificationLabel(
          "toto",
          0.5f
        )
      )
    )

    val projectId = "projectId"
    val defaultAlgorithmId = "algorithm id"

    val project = Project(
      projectId,
      "example project",
      ProjectConfiguration(
        Classification(),
        DoubleFeatures.featuresClass,
        10,
        Set(
          "label1",
          "label2",
          "label3"
        )
      ),
      Nil,
      DefaultAlgorithm(defaultAlgorithmId)
    )
    predictionsService.predict(
      request.features,
      project,
      request.algorithmId
    )
  }

}
