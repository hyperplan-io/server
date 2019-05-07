package com.foundaml.server.application.controllers

import com.foundaml.server.application.controllers.requests._
import com.foundaml.server.controllers.requests.PostProjectRequest
import com.foundaml.server.domain.models.errors._
import com.foundaml.server.domain.services.ProjectsService
import com.foundaml.server.infrastructure.serialization._
import com.foundaml.server.infrastructure.logging.IOLogging
import org.http4s.{HttpRoutes, HttpService}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import cats.Functor
import com.foundaml.server.domain.models.Project
import cats.effect.IO
import cats.implicits._

class ProjectsController(
    projectsService: ProjectsService
) extends Http4sDsl[IO]
    with IOLogging {

  import cats.MonadError
  val service: HttpRoutes[IO] = {

    HttpRoutes.of[IO] {
      case req @ POST -> Root =>
        (for {
          request <- req.as[PostProjectRequest](
            MonadError[IO, Throwable],
            PostProjectRequestSerializer.entityDecoder
          )
          project <- projectsService.createEmptyProject(request)
          _ <- logger.info(s"Project created with id ${project.id}")
        } yield project)
          .flatMap { project =>
            Created(ProjectSerializer.encodeJson(project))
          }
          .handleErrorWith {
            case ProjectAlreadyExists(projectId) =>
              Conflict(s"The project $projectId already exists")
            case InvalidProjectIdentifier(message) =>
              BadRequest(message)
            case FeaturesConfigurationError(message) =>
              BadRequest(message)
            case err =>
              logger.error(s"Unhandled error: ${err.getMessage}") *> InternalServerError(
                "An unknown error occurred"
              )
          }

      case GET -> Root / projectId =>
        projectsService
          .readProject(projectId)
          .flatMap {
            case Right(project) =>
              Ok(
                ProjectSerializer.encodeJson(
                  project
                )
              )
            case Left(err) =>
              NotFound()
          }
          .handleErrorWith {
            case ProjectDoesNotExist(_) =>
              NotFound(s"The project $projectId does not exist")
            case ProjectDataInconsistent(_) =>
              InternalServerError(
                s"The project $projectId has inconsistent data"
              )
            case err =>
              logger.error(s"Unhandled error: ${err.getMessage}") *> InternalServerError(
                "An unknown error occurred"
              )
          }
    }
  }

}
