package com.foundaml.server.application

import cats.effect
import cats.effect.Timer
import org.http4s.server.blaze.BlazeBuilder
import scalaz.zio.Task
import scalaz.zio.interop.catz._
import scalaz.zio.clock.Clock
import scalaz.zio.duration.Duration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, NANOSECONDS, TimeUnit}
import scala.util.Properties.envOrNone
import com.foundaml.server.application.controllers.{AlgorithmsController, ExamplesController, PredictionsController, ProjectsController}
import com.foundaml.server.domain.factories.ProjectFactory
import com.foundaml.server.domain.repositories.{AlgorithmsRepository, ProjectsRepository}
import com.foundaml.server.domain.services.{AlgorithmsService, PredictionsService, ProjectsService}

object Server {
  val port: Int = envOrNone("HTTP_PORT").fold(9090)(_.toInt)

  implicit val timer: Timer[Task] = new Timer[Task] {
    val zioClock = Clock.Live.clock

    override def clock: effect.Clock[Task] = new effect.Clock[Task] {
      override def realTime(unit: TimeUnit) =
        zioClock.nanoTime.map(unit.convert(_, NANOSECONDS))

      override def monotonic(unit: TimeUnit) = zioClock.currentTime(unit)
    }

    override def sleep(duration: FiniteDuration): Task[Unit] =
      zioClock.sleep(Duration.fromScala(duration))
  }

  def stream(
              predictionsService: PredictionsService,
              projectsService: ProjectsService,
              algorithmsService: AlgorithmsService,
              projectsRepository: ProjectsRepository
  )(implicit ec: ExecutionContext) =
    BlazeBuilder[Task]
      .bindHttp(8080, "0.0.0.0")
      .mountService(
        new PredictionsController(
          predictionsService
        ).service,
        "/predictions"
      )
      .mountService(
        new ProjectsController(
          projectsService
        ).service,
        "/projects"
      )
      .mountService(
        new AlgorithmsController(
          algorithmsService
        ).service,
        "/algorithms"
      )
      .mountService(
        new ExamplesController(
          predictionsService
        ).service,
        "/examples"
      )
      .serve

}
