package com.foundaml.server

import cats.effect
import cats.effect.Timer

import com.foundaml.server.services.domain._
import com.foundaml.server.controllers.{PredictionsHttpService, ProjectsHttpService}
import repositories._

import scalaz.zio.Task
import scalaz.zio.clock.Clock
import scalaz.zio.duration.Duration
import scalaz.zio.interop.catz.taskEffectInstances

import org.http4s.server.blaze.BlazeBuilder

import scala.concurrent.duration.{FiniteDuration, NANOSECONDS, TimeUnit}
import scala.util.Properties.envOrNone
import scala.concurrent.ExecutionContext

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
      ).mountService(
        new ProjectsHttpService(
          predictionsService,
          projectsRepository,
          algorithmsRepository
        ).service,
        "/projects"
      )
      .serve

}
