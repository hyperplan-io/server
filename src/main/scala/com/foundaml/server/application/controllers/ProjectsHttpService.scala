package com.foundaml.server.application.controllers

import org.http4s.{HttpService, _}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe._

import scalaz.zio.Task
import scalaz.zio.interop.catz._

import java.util.UUID

import com.foundaml.server.application.controllers.requests._
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.repositories._
import com.foundaml.server.domain.services.PredictionsService
import com.foundaml.server.infrastructure.serialization._

class ProjectsHttpService(
    predictionsService: PredictionsService,
    projectsRepository: ProjectsRepository,
    algorithmsRepository: AlgorithmsRepository
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
          project = Project(
            UUID.randomUUID().toString,
            request.name,
            ProjectConfiguration(
              request.configuration.problem,
              request.configuration.featureClass,
              request.configuration.featuresSize,
              request.configuration.labelsClass,
              request.configuration.labelsSize
            ),
            Nil,
            NoAlgorithm()
          )
          _ <- projectsRepository.insert(project)
        } yield project).flatMap { project =>
          Ok(ProjectSerializer.encodeJson(project))
        }
      case GET -> Root / projectId =>
        (for {
          project <- projectsRepository.read(projectId)
        } yield project).flatMap {
          case (
              id,
              name,
              Right(algorithmPolicy),
              Right(problem),
              featuresClass,
              featuresSize,
              labelsClass,
              labelsSize
              ) =>
            Ok(
              ProjectSerializer.encodeJson(
                Project(
                  id,
                  name,
                  ProjectConfiguration(
                    problem,
                    featuresClass,
                    featuresSize,
                    labelsClass,
                    labelsSize
                  ),
                  Nil,
                  algorithmPolicy
                )
              )
            )
          case _ =>
            BadRequest("there is an error with this project")
        }
    }
  }

}
