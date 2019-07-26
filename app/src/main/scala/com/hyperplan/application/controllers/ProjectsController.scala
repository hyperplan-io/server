package com.hyperplan.application.controllers

import cats.effect.IO
import cats.implicits._
import cats.{Functor, MonadError}
import com.hyperplan.application.controllers.requests._
import com.hyperplan.application.controllers.requests.{
  PatchProjectRequest,
  PostProjectRequest
}
import com.hyperplan.domain.errors.ProjectError.ProjectDoesNotExistError
import com.hyperplan.domain.errors._
import com.hyperplan.domain.services.ProjectsService
import com.hyperplan.infrastructure.serialization._
import com.hyperplan.infrastructure.serialization.errors._
import com.hyperplan.infrastructure.logging.IOLogging
import org.http4s.{HttpRoutes, HttpService}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import com.hyperplan.domain.models.Project
class ProjectsController(
    projectsService: ProjectsService
) extends Http4sDsl[IO]
    with IOLogging {

  val unhandledErrorMessage =
    s"""Unhandled server error, please check the logs or contact support"""

  val service: HttpRoutes[IO] = {

    HttpRoutes.of[IO] {
      case req @ POST -> Root =>
        (for {
          request <- req.as[PostProjectRequest](
            MonadError[IO, Throwable],
            PostProjectRequestSerializer.entityDecoder
          )
          project <- projectsService.createEmptyProject(request).value
        } yield project)
          .flatMap {
            case Right(project) =>
              logger.info(s"Project created with id ${project.id}") >> Created(
                ProjectSerializer.encodeJson(project)
              )
            case Left(errors) =>
              BadRequest(
                ProjectErrorsSerializer.encodeJson(errors.toList: _*)
              )
          }
          .handleErrorWith {
            case err =>
              logger.warn("Unhandled error in ProjectsController") >> InternalServerError(
                unhandledErrorMessage
              )
          }

      case req @ PATCH -> Root / projectId =>
        (for {
          request <- req.as[PatchProjectRequest](
            MonadError[IO, Throwable],
            PatchProjectRequestSerializer.entityDecoder
          )
          project <- projectsService.updateProject(
            projectId,
            request.name,
            request.policy
          )
          _ <- logger.info(s"Project ${projectId} updated")
        } yield project)
          .flatMap { project =>
            NoContent()
          }
          .handleErrorWith {
            case ProjectError.ProjectDoesNotExistError(projectId) =>
              NotFound(s"The project $projectId does not exist")
            case ProjectError.ProjectDataInconsistentError(projectId) =>
              logger.error(s"The project $projectId has inconsistent data") *>
                InternalServerError(s"The project data is inconsistent")
            case err =>
              logger.error(s"Unhandled error: ${err}") *> InternalServerError(
                "An unknown error occurred"
              )
          }

      case GET -> Root =>
        projectsService.readProjects
          .flatMap { projects =>
            Ok(
              ProjectSerializer.encodeJsonList(
                projects
              )
            )
          }
          .handleErrorWith {
            case err =>
              logger.error(s"Unhandled error: ${err}") *> InternalServerError(
                "An unknown error occurred"
              )
          }

      case GET -> Root / projectId =>
        projectsService
          .readProject(projectId)
          .flatMap {
            case Some(project) =>
              Ok(
                ProjectSerializer.encodeJson(
                  project
                )
              )
            case None =>
              NotFound(
                ProjectErrorsSerializer.encodeJson(
                  ProjectDoesNotExistError(
                    ProjectDoesNotExistError.message(projectId)
                  )
                )
              )
          }
          .handleErrorWith {
            case ProjectError.ProjectDataInconsistentError(_) =>
              InternalServerError(
                s"The project $projectId has inconsistent data"
              )
            case err =>
              logger.error(s"Unhandled error: ${err}") *> InternalServerError(
                "An unknown error occurred"
              )
          }
    }
  }

}
