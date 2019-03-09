package com.foundaml.server

import cats.data.Kleisli
import cats.effect
import cats.effect.Timer
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.{Request, Response}
import scalaz.zio.Task
import scalaz.zio.clock.Clock
import scalaz.zio.duration.Duration
import scalaz.zio.interop.catz.taskEffectInstances

import org.http4s.server.blaze.BlazeBuilder
import scala.concurrent.duration.{FiniteDuration, NANOSECONDS, TimeUnit}
import scala.util.Properties.envOrNone
import services.http.PredictionService
import scala.concurrent.ExecutionContext

import services.serialization.CirceEncoders._

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

  def predictionService = new PredictionService[Task].service

  def stream(implicit ec: ExecutionContext) =
    BlazeBuilder[Task]
      .bindHttp(8080, "0.0.0.0")
      .mountService(predictionService, "/predictions")
      .serve

}
