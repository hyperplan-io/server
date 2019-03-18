package com.foundaml.server.application.controllers

import com.foundaml.server.application.controllers.requests._
import com.foundaml.server.domain.factories.ProjectFactory
import com.foundaml.server.domain.services.{PredictionsService, ProjectsService}
import com.foundaml.server.infrastructure.serialization._
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import scalaz.zio.Task
import scalaz.zio.interop.catz._

class ProjectsController(
    predictionsService: PredictionsService,
    projectsService: ProjectsService,
    projectFactory: ProjectFactory
) extends Http4sDsl[Task] {


  val service: HttpService[Task] = {

    HttpService[Task] {
      case req @ POST -> Root =>
        (for {
          request <- req
            .attemptAs[PostProjectRequest](
              PostProjectRequestEntitySerializer.entityDecoder
            )
            .fold(throw _, identity)

          project <- projectsService.createEmptyProject(
            request.id,
            request.name,
            request.configuration.problem,
            request.configuration.featuresClass,
            request.configuration.featuresSize,
            request.configuration.labels,
          )
        } yield project).flatMap { project =>
          Ok(ProjectSerializer.encodeJson(project))
        }

      case GET -> Root / projectId =>
        projectsService.readProject(projectId).flatMap { project =>
          Ok(
            ProjectSerializer.encodeJson(
              project
            )
          )
        }
    }
  }

}
