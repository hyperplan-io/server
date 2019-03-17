package com.foundaml.server.application.controllers

import java.util.UUID

import com.foundaml.server.application.controllers.requests._
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.repositories._
import com.foundaml.server.domain.services.PredictionsService
import com.foundaml.server.infrastructure.serialization._
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import scalaz.zio.Task
import scalaz.zio.interop.catz._

class AlgorithmsHttpService(
    algorithmsRepository: AlgorithmsRepository,
    projectsRepository: ProjectsRepository
) extends Http4sDsl[Task] {

  val service: HttpService[Task] = {
    HttpService[Task] {
      case req @ POST -> Root =>
        (for {
          request <- req
            .attemptAs[PostAlgorithmRequest](
            PostAlgorithmRequestEntitySerializer.entityDecoder
            )
            .fold(throw _, identity)
          _ <- projectsRepository.read(request.projectId)
          algorithm = Algorithm(
            UUID.randomUUID().toString,
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
