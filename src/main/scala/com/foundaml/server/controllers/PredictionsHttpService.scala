package com.foundaml.server.controllers

import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpService, _}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import scalaz.zio.Task
import scalaz.zio.interop.catz._
import com.foundaml.server.controllers.requests._
import com.foundaml.server.models._
import com.foundaml.server.models.backends._
import com.foundaml.server.models.features._
import com.foundaml.server.models.labels._
import com.foundaml.server.repositories._
import com.foundaml.server.services.domain.PredictionsService
import com.foundaml.server.services.infrastructure.serialization.{LabelsSerializer, PredictionRequestSerializer}


class PredictionsHttpService(
  predictionsService: PredictionsService,
  projectsRepository: ProjectsRepository,
  algorithmsRepository: AlgorithmsRepository
)
    extends Http4sDsl[Task] {

  implicit val decoder = PredictionRequestSerializer.decoder
  implicit val requestDecoder: EntityDecoder[Task, PredictionRequest] =
    jsonOf[Task, PredictionRequest]

  val service: HttpService[Task] = {
    HttpService[Task] {
      case req @ POST -> Root =>
        Ok(for {
          predictionRequest <- req.as[PredictionRequest]
          labels <- predict(predictionRequest)
          _ = println(labels)
        } yield LabelsSerializer.encodeJson(labels))
    }
  }

  def predict(request: PredictionRequest) = {

    val computed = TensorFlowClassificationLabels(
        List(
          TensorFlowClassicationLabel(
            List(1, 2, 3),
            List(0.0f, 0.1f, 0.2f),
            List("class1", "class2", "class3"),
            List(0.0f, 0.0f, 0.0f)
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
