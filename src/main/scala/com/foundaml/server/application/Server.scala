package com.foundaml.server.application

import cats.effect.Timer
import cats.effect.IO
import cats.implicits._

import org.http4s.server.blaze.{BlazeBuilder, BlazeServerBuilder}
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

  /*
  implicit val timer: Timer[IO] = new Timer[IO] {
    val zioClock = Clock.Live.clock

    override def clock: effect.Clock[IO] = new effect.Clock[IO] {
      override def realTime(unit: TimeUnit) =
        zioClock.nanoTime.map(unit.convert(_, NANOSECONDS))

      override def monotonic(unit: TimeUnit) = zioClock.currentTime(unit)
    }

    override def sleep(duration: FiniteDuration): IO[Unit] =
      zioClock.sleep(Duration.fromScala(duration))
  }
   */

  import org.http4s.implicits._

  import cats.effect.ContextShift
  def stream(
      predictionsService: PredictionsService,
      projectsService: ProjectsService,
      algorithmsService: AlgorithmsService,
      projectsRepository: ProjectsRepository,
      port: Int
  )(implicit cs: ContextShift[IO], timer: Timer[IO]) =
    BlazeServerBuilder[IO]
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
