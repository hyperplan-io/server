package com.foundaml.server.application.controllers


import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

import scalaz.zio.Task
import scalaz.zio.interop.catz._

import com.foundaml.server.application.controllers.requests._
import com.foundaml.server.domain.factories.ProjectFactory
import com.foundaml.server.domain.models.labels._
import com.foundaml.server.domain.repositories._
import com.foundaml.server.domain.services.PredictionsService
import com.foundaml.server.infrastructure.serialization.{LabelsSerializer, PredictionRequestEntitySerializer}

class PredictionsHttpService(
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
          _ <- Task(println("deserialized features"))
          labels <- predict(predictionRequest)
          _ <- Task(println("prediction successful"))
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
  ): Task[Either[Throwable, Labels]] = {
    projectFactory.get(request.projectId).flatMap { project =>
        predictionsService.predict(
          request.features,
          project,
          request.algorithmId
        )
      }
  }

}
