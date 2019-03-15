package com.foundaml.server.application.controllers

import java.util.UUID

import com.foundaml.server.application.controllers.requests._
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.repositories._
import com.foundaml.server.domain.services.PredictionsService
import com.foundaml.server.infrastructure.serialization._
import io.circe.generic.extras.auto._
import org.http4s.{HttpService, _}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import scalaz.zio.Task
import scalaz.zio.interop.catz._

class ProjectsHttpService(
    predictionsService: PredictionsService,
    projectsRepository: ProjectsRepository,
    algorithmsRepository: AlgorithmsRepository
) extends Http4sDsl[Task] {

  implicit val discriminator = CirceEncoders.discriminator
  implicit val requestDecoder: EntityDecoder[Task, PostProjectRequest] =
    jsonOf[Task, PostProjectRequest]
  val service: HttpService[Task] = {
    HttpService[Task] {
      case req @ POST -> Root =>
        (for {
          request <- req.as[PostProjectRequest]
          project = Project(
            UUID.randomUUID().toString,
            request.name,
            request.problem,
            request.featureType,
            request.labelType,
            Map.empty,
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
              Right(problem),
              Right(algorithmPolicy),
              featureClass,
              labelClass
              ) =>
            Ok(
              ProjectSerializer.encodeJson(
                Project(
                  id,
                  name,
                  problem,
                  featureClass,
                  labelClass,
                  Map.empty,
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
