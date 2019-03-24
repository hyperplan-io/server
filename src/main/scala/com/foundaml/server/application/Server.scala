package com.foundaml.server.application

import cats.effect
import cats.effect.Timer
import org.http4s.server.blaze.{BlazeBuilder, BlazeServerBuilder}
import scalaz.zio.Task
import scalaz.zio.interop.catz._
import scalaz.zio.clock.Clock
import scalaz.zio.duration.Duration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, NANOSECONDS, TimeUnit}
import scala.util.Properties.envOrNone
import com.foundaml.server.application.controllers.{
  AlgorithmsController,
  ExamplesController,
  PredictionsController,
  ProjectsController
}
import com.foundaml.server.domain.factories.ProjectFactory
import com.foundaml.server.domain.repositories.{
  AlgorithmsRepository,
  ProjectsRepository
}
import com.foundaml.server.domain.services.{
  AlgorithmsService,
  PredictionsService,
  ProjectsService
}
import org.http4s.server.Router

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

  import org.http4s.implicits._

  def stream(
      predictionsService: PredictionsService,
      projectsService: ProjectsService,
      algorithmsService: AlgorithmsService,
      projectsRepository: ProjectsRepository,
      port: Int
  )(implicit ec: ExecutionContext) =
    BlazeServerBuilder[Task]
      .bindHttp(port, "0.0.0.0")
      .withHttpApp(
        Router(
          "/predictions" -> new PredictionsController(
            predictionsService
          ).service,
          "/projects" -> new ProjectsController(
            projectsService
          ).service,
          "/algorithms" -> new AlgorithmsController(
            algorithmsService
          ).service,
          "/examples" -> new ExamplesController(
            predictionsService
          ).service
        ).orNotFound
      )
      .serve

}
