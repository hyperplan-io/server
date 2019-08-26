/*
 *
 * This file is part of the Hyperplan project.
 * Copyright (C) 2019  Hyperplan
 * Authors: Antoine Sauray
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gn/u.org/licenses/>.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the Hyperplan software without
 * disclosing the source code of your own applications.
 *
 *
 */

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
          project <- projectsService.createProject(request).value
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
          .handleErrorWith { err =>
            logger.warn("Unhandled error in ProjectsController", err) >> InternalServerError(
              unhandledErrorMessage
            )
          }

      case req @ PATCH -> Root / projectId =>
        (for {
          request <- req.as[PatchProjectRequest](
            MonadError[IO, Throwable],
            PatchProjectRequestSerializer.entityDecoder
          )
          project <- projectsService
            .updateProject(
              projectId,
              request.name,
              request.policy
            )
            .value
          _ <- logger.info(s"Project ${projectId} updated")
        } yield project)
          .flatMap {
            case Right(project) =>
              Ok(
                ProjectSerializer.encodeJson(project)
              )
            case Left(errors) =>
              BadRequest(
                ProjectErrorsSerializer.encodeJson(errors.toList: _*)
              )
          }
          .handleErrorWith { err =>
            logger.warn("Unhandled error in ProjectsController", err) >> InternalServerError(
              unhandledErrorMessage
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
          .handleErrorWith { err =>
            logger.warn(s"Unhandled error in ProjectsController", err) *> InternalServerError(
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
              logger.warn(s"Unhandled error in ProjectsController", err) *> InternalServerError(
                "An unknown error occurred"
              )
          }

      case req @ PUT -> Root / projectId / "algorithms" / algorithmId =>
        (for {
          request <- req.as[PostAlgorithmRequest](
            MonadError[IO, Throwable],
            PostAlgorithmRequestEntitySerializer.entityDecoder
          )
          algorithm <- projectsService
            .createAlgorithm(
              algorithmId,
              request.backend,
              projectId,
              request.security
            )
            .value
        } yield algorithm)
          .flatMap {
            case Right(algorithm) =>
              logger.info(
                s"Algorithm created with id ${algorithm.id} on project ${algorithm.projectId}"
              ) *> Created(AlgorithmsSerializer.encodeJson(algorithm))
            case Left(errors) =>
              val errorList = errors.toList
              logger.warn(
                s"Failed to create algorithm: ${errorList.mkString(",")}"
              ) *> BadRequest(
                AlgorithmErrorsSerializer.encodeJson(errorList: _*)
              )
          }
          .handleErrorWith { err =>
            logger.warn(s"Unhandled error in AlgorithmsController", err) *> InternalServerError(
              "An unknown error occurred"
            )
          }

      case _ @DELETE -> Root / projectId =>
        projectsService.deleteProject(projectId).flatMap {
          case count if count > 0 =>
            Ok()
          case count if count <= 0 =>
            NotFound()
        }

      case _ @DELETE -> Root / projectId / "algorithms" / algorithmId =>
        projectsService.deleteAlgorithm(projectId, algorithmId).flatMap {
          case count if count > 0 =>
            Ok()
          case count if count <= 0 =>
            NotFound()
        }
    }
  }

}
