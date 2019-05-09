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
import com.foundaml.server.domain.factories.ProjectFactory
import com.foundaml.server.domain.repositories.{
  AlgorithmsRepository,
  ProjectsRepository
}

import com.foundaml.server.infrastructure.streaming._
import com.foundaml.server.domain.services._
import org.http4s.server.Router
import kamon.http4s.middleware.server.KamonSupport

object Server {
  val port: Int = envOrNone("HTTP_PORT").fold(9090)(_.toInt)

  import org.http4s.implicits._
  import cats.effect.ContextShift

  def stream(
      predictionsService: PredictionsService,
      projectsService: ProjectsService,
      algorithmsService: AlgorithmsService,
      domainService: DomainService,
      kafkaService: Option[KafkaService],
      projectsRepository: ProjectsRepository,
      port: Int
  )(implicit cs: ContextShift[IO], timer: Timer[IO], xa: Transactor[IO]) =
    BlazeServerBuilder[IO]
      .bindHttp(port, "0.0.0.0")
      .withHttpApp(
        Router(
          "/predictions" -> KamonSupport(
            new PredictionsController(
              predictionsService
            ).service
          ),
          "/projects" -> KamonSupport(
            new ProjectsController(
              projectsService
            ).service
          ),
          "/algorithms" -> KamonSupport(
            new AlgorithmsController(
              algorithmsService
            ).service
          ),
          "/examples" -> KamonSupport(
            new ExamplesController(
              predictionsService
            ).service
          ),
          "/features" -> KamonSupport(
            new FeaturesController(
              domainService
            ).service
          ),
          "/labels" -> KamonSupport(
            new LabelsController(
              domainService
            ).service
          ),
          "/_health" -> KamonSupport(
            new HealthController(
              xa,
              kafkaService
            ).service
          )
        ).orNotFound
      )
      .serve
}
