package com.foundaml.server.application.controllers

import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import scalaz.zio.Task
import scalaz.zio.interop.catz._
import com.foundaml.server.application.controllers.requests._
import com.foundaml.server.domain.factories.ProjectFactory
import com.foundaml.server.domain.models.Prediction
import com.foundaml.server.domain.repositories._
import com.foundaml.server.domain.services.PredictionsService
import com.foundaml.server.infrastructure.serialization.{
  PredictionRequestEntitySerializer,
  PredictionSerializer
}

class PredictionsController(
    predictionsService: PredictionsService,
    projectsRepository: ProjectsRepository,
    algorithmsRepository: AlgorithmsRepository,
    projectFactory: ProjectFactory
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
          prediction <- predict(predictionRequest)
          _ <- Task(
            println(
              s"Prediction computed for project ${prediction.projectId} using algorithm ${prediction.algorithmId}"
            )
          )
        } yield prediction).flatMap { prediction =>
          Ok(PredictionSerializer.encodeJson(prediction))
        }
    }
  }

  def predict(
      request: PredictionRequest
  ): Task[Prediction] = {
    predictionsService.predict(
      request.projectId,
      request.features,
      request.algorithmId
    )

  }

}
