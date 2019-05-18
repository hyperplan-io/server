package com.foundaml.server.application

import cats.effect.Timer
import cats.effect.IO
import cats.implicits._

import doobie._

import org.http4s.server.blaze.{BlazeBuilder, BlazeServerBuilder}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, NANOSECONDS, TimeUnit}
import scala.util.Properties.envOrNone
import com.foundaml.server.application.controllers._
import com.foundaml.server.domain.repositories.{
  AlgorithmsRepository,
  ProjectsRepository
}

import com.foundaml.server.infrastructure.streaming._
import com.foundaml.server.domain.services._
import org.http4s.server.Router

object Server {
  val port: Int = envOrNone("HTTP_PORT").fold(9090)(_.toInt)

  import org.http4s.implicits._
  import cats.effect.ContextShift
  import com.foundaml.server.infrastructure.auth.AuthenticationService

  def stream(
      predictionsService: PredictionsService,
      projectsService: ProjectsService,
      algorithmsService: AlgorithmsService,
      domainService: DomainService,
      kafkaService: Option[KafkaService],
      projectsRepository: ProjectsRepository,
      port: Int
  )(
      implicit cs: ContextShift[IO],
      timer: Timer[IO],
      xa: Transactor[IO],
      publicKey: AuthenticationService.PublicKey,
      privateKey: AuthenticationService.PrivateKey
  ) = {
    val predictionsController = new PredictionsController(
      predictionsService
    )
    val projectsController = new ProjectsController(
      projectsService
    )
    val algorithmsController = new AlgorithmsController(
      algorithmsService
    )
    val examplesController = new ExamplesController(
      predictionsService
    )
    val featuresController = new FeaturesController(
      domainService
    )
    val labelsController = new LabelsController(
      domainService
    )
    val healthController = new HealthController(
      xa,
      kafkaService
    )

    BlazeServerBuilder[IO]
      .bindHttp(port, "0.0.0.0")
      .withHttpApp(
        Router(
          "/predictions" -> (
            predictionsController.service <+> AuthenticationMiddleware
              .jwtAuthenticate(predictionsController.service)
          ),
          "/projects" -> (
            projectsController.service <+> AuthenticationMiddleware
              .jwtAuthenticate(projectsController.service)
          ),
          "/algorithms" -> (
            algorithmsController.service <+> AuthenticationMiddleware
              .jwtAuthenticate(algorithmsController.service)
          ),
          "/examples" -> (
            examplesController.service <+> AuthenticationMiddleware
              .jwtAuthenticate(examplesController.service)
          ),
          "/features" -> (
            featuresController.service <+> AuthenticationMiddleware
              .jwtAuthenticate(featuresController.service)
          ),
          "/labels" -> (
            labelsController.service <+> AuthenticationMiddleware
              .jwtAuthenticate(labelsController.service)
          ),
          "/_health" -> (
            healthController.service
          )
        ).orNotFound
      )
      .serve
  }

}
