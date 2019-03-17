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
import com.foundaml.server.application.controllers.{
  AlgorithmsHttpService,
  PredictionsHttpService,
  ProjectsHttpService
}
import com.foundaml.server.domain.repositories.{
  AlgorithmsRepository,
  ProjectsRepository
}
import com.foundaml.server.domain.services.PredictionsService

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
      projectsRepository: ProjectsRepository,
      algorithmsRepository: AlgorithmsRepository
  )(implicit ec: ExecutionContext) =
    BlazeBuilder[Task]
      .bindHttp(8080, "0.0.0.0")
      .mountService(
        new PredictionsHttpService(
          predictionsService,
          projectsRepository,
          algorithmsRepository
        ).service,
        "/predictions"
      )
      .mountService(
        new ProjectsHttpService(
          predictionsService,
          projectsRepository,
          algorithmsRepository
        ).service,
        "/projects"
      )
      .mountService(
        new AlgorithmsHttpService(
          projectsRepository,
          algorithmsRepository
        ).service,
        "/algorithms"
      )
      .serve

}
