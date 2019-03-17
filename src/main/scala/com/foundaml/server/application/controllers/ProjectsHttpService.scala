package com.foundaml.server.application.controllers

import org.http4s.{HttpService, _}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe._
import scalaz.zio.Task
import scalaz.zio.interop.catz._
import java.util.UUID

import com.foundaml.server.application.controllers.requests._
import com.foundaml.server.domain.factories.ProjectFactory
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.errors.InvalidArgument
import com.foundaml.server.domain.repositories._
import com.foundaml.server.domain.services.PredictionsService
import com.foundaml.server.infrastructure.serialization._

class ProjectsHttpService(
    predictionsService: PredictionsService,
    projectsRepository: ProjectsRepository,
    algorithmsRepository: AlgorithmsRepository,
    projectFactory: ProjectFactory
) extends Http4sDsl[Task] {

  val regex = "[0-9a-zA-Z-_]*"
  def validateAlphaNumerical(input: String): Option[String] = {
    if (input.matches(regex)) {
      None
    } else {
      Some(s"The identifier $input is not alphanumerical")
    }
  }

  def validateRequest(request: PostProjectRequest): List[String] = {
    List(
      validateAlphaNumerical(request.id)
    ).flatten
  }

  val service: HttpService[Task] = {

    HttpService[Task] {
      case req @ POST -> Root =>
        (for {
          request <- req
            .attemptAs[PostProjectRequest](
              PostProjectRequestEntitySerializer.entityDecoder
            )
            .fold(throw _, identity)
          errors = validateRequest(request)
          _ <- if (errors.isEmpty) {
            Task(Unit)
          } else {
            Task.fail(
              InvalidArgument(
                errors.mkString(
                  s"The following errors occurred: ${errors.mkString(", ")}"
                )
              )
            )
          }
          project = Project(
            request.id,
            request.name,
            ProjectConfiguration(
              request.configuration.problem,
              request.configuration.featuresClass,
              request.configuration.featuresSize,
              request.configuration.labels
            ),
            Nil,
            NoAlgorithm()
          )
          _ <- projectsRepository.insert(project)
        } yield project).flatMap { project =>
          Ok(ProjectSerializer.encodeJson(project))
        }
      case GET -> Root / projectId =>
        projectFactory.get(projectId).flatMap { project =>
          Ok(
            ProjectSerializer.encodeJson(
              project
            )
          )
        }
    }
  }

}
