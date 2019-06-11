package com.hyperplan.application

import cats.effect.Timer
import cats.effect.IO
import cats.implicits._

import doobie._

import org.http4s.server.blaze.{BlazeBuilder, BlazeServerBuilder}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, NANOSECONDS, TimeUnit}
import scala.util.Properties.envOrNone
import com.hyperplan.application.controllers._
import com.hyperplan.domain.repositories.{
  AlgorithmsRepository,
  ProjectsRepository
}

import com.hyperplan.infrastructure.streaming._
import com.hyperplan.domain.services._
import org.http4s.server.Router

object Server {
  val port: Int = envOrNone("HTTP_PORT").fold(9090)(_.toInt)

  import org.http4s.implicits._
  import cats.effect.ContextShift
  import com.hyperplan.infrastructure.auth.AuthenticationService

  def stream(
      predictionsService: PredictionsService,
      projectsService: ProjectsService,
      algorithmsService: AlgorithmsService,
      domainService: DomainService,
      privacyService: PrivacyService,
      kafkaService: Option[KafkaService],
      projectsRepository: ProjectsRepository,
      port: Int
  )(
      implicit cs: ContextShift[IO],
      timer: Timer[IO],
      xa: Transactor[IO],
      config: ApplicationConfig,
      publicKey: AuthenticationService.PublicKey,
      privateKey: AuthenticationService.PrivateKey
  ) = {
    val predictionsController = new PredictionsController(
      projectsService,
      domainService,
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
    val authControler = new AuthenticationController(
      config.credentials,
      publicKey,
      privateKey
    )
    val privacyController = new PrivacyController(
      privacyService
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
            AuthenticationMiddleware
              .jwtAuthenticate(
                predictionsController.service,
                AuthenticationService.PredictionScope
              )
            ),
          "/projects" -> (
            AuthenticationMiddleware
              .jwtAuthenticate(
                projectsController.service,
                AuthenticationService.AdminScope
              )
            ),
          "/algorithms" -> (
            AuthenticationMiddleware
              .jwtAuthenticate(
                algorithmsController.service,
                AuthenticationService.AdminScope
              )
            ),
          "/examples" -> (
            AuthenticationMiddleware
              .jwtAuthenticate(
                examplesController.service,
                AuthenticationService.PredictionScope
              )
            ),
          "/features" -> (
            AuthenticationMiddleware.jwtAuthenticate(
              featuresController.service,
              AuthenticationService.AdminScope
            )
          ),
          "/labels" -> (
            AuthenticationMiddleware
              .jwtAuthenticate(
                labelsController.service,
                AuthenticationService.AdminScope
              )
            ),
          "/privacy" -> (
            AuthenticationMiddleware
              .jwtAuthenticate(
                privacyController.service,
                AuthenticationService.AdminScope
              )
            ),
          "/authentication" -> (
            authControler.service
          ),
          "/_health" -> (
            healthController.service
          )
        ).orNotFound
      )
      .serve
  }

}
