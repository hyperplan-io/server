package com.foundaml.server.application.controllers

import cats.Functor
import com.foundaml.server.application.controllers.requests._
import com.foundaml.server.domain.models.errors.{
  FeaturesConfigurationError,
  InvalidProjectIdentifier,
  ProjectAlreadyExists,
  ProjectNotFound
}
import com.foundaml.server.domain.services.ProjectsService
import com.foundaml.server.infrastructure.serialization._
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import scalaz.zio.Task
import scalaz.zio.interop.catz._

class ProjectsController(
    projectsService: ProjectsService
) extends Http4sDsl[Task] {

  val service: HttpService[Task] = {

    HttpService[Task] {
      case req @ POST -> Root =>
        (for {
          request <- req.as[PostProjectRequest](
            Functor[Task],
            PostProjectRequestEntitySerializer.entityDecoder
          )
          project <- projectsService.createEmptyProject(
            request.id,
            request.name,
            request.configuration.problem,
            request.configuration.features,
            request.configuration.labels
          )
        } yield project)
          .flatMap { project =>
            Created(ProjectSerializer.encodeJson(project))
          }
          .catchAll {
            case ProjectAlreadyExists(projectId) =>
              Conflict(s"The project $projectId already exists")
            case InvalidProjectIdentifier(message) =>
              BadRequest(message)
            case FeaturesConfigurationError(message) =>
              BadRequest(message)
            case err =>
              InternalServerError("An unknown error occurred")
          }

      case GET -> Root / projectId =>
        projectsService
          .readProject(projectId)
          .flatMap { project =>
            Ok(
              ProjectSerializer.encodeJson(
                project
              )
            )
          }
          .catchAll {
            case ProjectNotFound(_) =>
              NotFound(s"The project $projectId does not exist")
          }
    }
  }

}
