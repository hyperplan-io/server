package com.foundaml.server.services.infrastructure.http

import io.circe.Json
import org.http4s.HttpService
import org.http4s.dsl.Http4sDsl

import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._

import scalaz.zio.Task
import scalaz.zio.interop.catz._

import com.foundaml.server.services.infrastructure.http.requests._
import com.foundaml.server.services.infrastructure.serialization.CirceEncoders._
import com.foundaml.server.services.infrastructure.serialization.ProblemTypeSerializer._

import com.foundaml.server.services.domain.PredictionsService

import com.foundaml.server.services.domain._
import com.foundaml.server.models.features._
import com.foundaml.server.models.labels._
import com.foundaml.server.models.backends._
import com.foundaml.server.models._
import com.foundaml.server.services.infrastructure.http.requests._
import com.foundaml.server.repositories._

import java.util.UUID

class ProjectsHttpService(
  predictionsService: PredictionsService,
  projectsRepository: ProjectsRepository,
  algorithmsRepository: AlgorithmsRepository
)
    extends Http4sDsl[Task] {

    implicit val requestDecoder: EntityDecoder[Task, PostProjectRequest] = jsonOf[Task, PostProjectRequest]
  val service: HttpService[Task] = {
    HttpService[Task] {
      case req @ POST -> Root =>
        Ok(for {
          request <- req.as[PostProjectRequest]
          _ <- projectsRepository.insert(
              Project(
                UUID.randomUUID().toString,
                request.name,
                Classification,
                request.featureType,
                request.labelType,
                Map.empty,
                NoAlgorithm()
              )
            )
          _ = println(request)
        } yield Json.fromString(""))
    }
  }

}
