package com.foundaml.server.application.controllers

import org.http4s.{HttpService, _}
import org.http4s.dsl.Http4sDsl

import scalaz.zio.Task
import scalaz.zio.interop.catz._

import com.foundaml.server.application.controllers.requests._
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.backends._
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
          labelsJson = LabelsSerializer.encodeJson(labels)
        } yield labelsJson).flatMap(Ok(_))
    }
  }

  def predict(request: PredictionRequest): Task[Labels] = {

    val computed = Labels(
      Set(
        ClassificationLabel(
          "toto",
          0.5f
        )
      )
    )

    val projectId = "projectId"
    val defaultAlgorithm = Algorithm(
      "algorithm id",
      Local(computed),
      projectId
    )

    val project = Project(
      projectId,
      "example project",
      Classification(),
      "tf.cl",
      "tf.cl",
      Map.empty,
      DefaultAlgorithm(defaultAlgorithm)
    )
    predictionsService.predict(
      request.features,
      project,
      request.algorithmId
    )
  }

}
