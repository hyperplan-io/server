package com.foundaml.server.application.controllers

import java.util.UUID

import com.foundaml.server.application.controllers.requests._
import com.foundaml.server.domain.factories.ProjectFactory
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.errors.InvalidArgument
import com.foundaml.server.domain.models.features.transformers.TensorFlowFeaturesTransformer
import com.foundaml.server.domain.models.labels.transformers.TensorFlowLabelsTransformer
import com.foundaml.server.domain.repositories._
import com.foundaml.server.domain.services.PredictionsService
import com.foundaml.server.infrastructure.serialization._
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import scalaz.zio.Task
import scalaz.zio.interop.catz._

class AlgorithmsHttpService(
    projectsRepository: ProjectsRepository,
    algorithmsRepository: AlgorithmsRepository,
    projectFactory: ProjectFactory
) extends Http4sDsl[Task] {

  def validateEqualSize(
      expectedSize: Int,
      actualSize: Int,
      featureName: String
  ) =
    if (expectedSize != actualSize) {
      Some(s"The $featureName size is incorrect for the project")
    } else {
      None
    }

  def validate(request: PostAlgorithmRequest, project: Project) = {
    request.backend match {
      case com.foundaml.server.domain.models.backends.Local(computed) => Nil
      case com.foundaml.server.domain.models.backends.TensorFlowBackend(
          _,
          _,
          TensorFlowFeaturesTransformer(signatureName, fields),
          TensorFlowLabelsTransformer(labels)
          ) =>
        List(
          validateEqualSize(
            project.configuration.featuresSize,
            fields.size,
            "features"
          ),
          validateEqualSize(
            project.configuration.labels.size,
            labels.size,
            "labels"
          )
        ).flatten
    }
  }

  val service: HttpService[Task] = {
    HttpService[Task] {
      case req @ POST -> Root =>
        (for {
          request <- req
            .attemptAs[PostAlgorithmRequest](
              PostAlgorithmRequestEntitySerializer.entityDecoder
            )
            .fold(throw _, identity)
          project <- projectFactory.get(request.projectId)
          errors = validate(request, project)
          _ <- if (errors.isEmpty) {
            Task.succeed(Unit)
          } else {
            Task.fail(
              InvalidArgument(
                s"This algorithm cannot be added because: ${errors.mkString(", ")}"
              )
            )
          }
          algorithm = Algorithm(
            request.id,
            request.backend,
            request.projectId
          )
          _ <- algorithmsRepository.insert(algorithm)
        } yield algorithm).flatMap { algorithm =>
          Ok(AlgorithmsSerializer.encodeJson(algorithm))
        }
    }
  }

}
