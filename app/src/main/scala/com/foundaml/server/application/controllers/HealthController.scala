package com.foundaml.server.application.controllers

import cats.Functor
import org.http4s.{HttpRoutes, HttpService}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import cats.effect.IO
import cats.implicits._
import cats.MonadError

import com.foundaml.server.infrastructure.logging.IOLogging

import com.foundaml.server.infrastructure.storage.PostgresqlService
import doobie._

import cats.effect.Timer
import cats.effect.ContextShift
import com.foundaml.server.infrastructure.streaming.KafkaService


import sup._, sup.modules.doobie._
import eu.timepit.refined.auto._
import scala.concurrent.duration._
class HealthController(
  xa: Transactor[IO],
  kafkaService: Option[KafkaService]
)(implicit cs: ContextShift[IO], timer: Timer[IO])
    extends Http4sDsl[IO]
    with IOLogging {

  import java.util.concurrent.TimeUnit
  import scala.concurrent.duration.Duration
  import org.http4s.Response
  val service: HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case req @ GET -> Root =>
        HealthReporter.fromChecks(
          databaseCheck(xa).through(mods.timeoutToSick(4.seconds)),
          kafkaCheck(kafkaService).through(mods.timeoutToSick(3.seconds))
        ).check.flatMap { healthResult => 
          healthResult.value.health match {
            case Health.Healthy => logger.debug("healthcheck successful") *> Ok()
            case Health.Sick => logger.warn("healthcheck failed") *> InternalServerError()
          }
        }
    }
  }

  def databaseCheck(implicit xa: Transactor[IO]) = 
    connectionCheck(xa)(timeoutSeconds = Some(4))

  def kafkaCheck(kafkaService: Option[KafkaService]) = 
    kafkaService.fold(
      HealthCheck.liftFBoolean(IO.pure(true))
      )(service => HealthCheck.liftFBoolean(service.isHealthy ))
}
