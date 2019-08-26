/*
 *
 * This file is part of the Hyperplan project.
 * Copyright (C) 2019  Hyperplan
 * Authors: Antoine Sauray
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gn/u.org/licenses/>.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the Hyperplan software without
 * disclosing the source code of your own applications.
 *
 *
 */

package com.hyperplan.application.controllers

import cats.Functor
import org.http4s.{HttpRoutes, HttpService}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import cats.effect.IO
import cats.implicits._
import cats.MonadError

import com.hyperplan.infrastructure.logging.IOLogging

import com.hyperplan.infrastructure.storage.PostgresqlService
import doobie._

import cats.effect.Timer
import cats.effect.ContextShift
import com.hyperplan.infrastructure.streaming.KafkaService

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
        HealthReporter
          .fromChecks(
            databaseCheck(xa).through(mods.timeoutToSick(4.seconds)),
            kafkaCheck(kafkaService).through(mods.timeoutToSick(3.seconds))
          )
          .check
          .flatMap { healthResult =>
            healthResult.value.health match {
              case Health.Healthy =>
                logger.debug("healthcheck successful") *> Ok()
              case Health.Sick =>
                logger.warn("healthcheck failed") *> InternalServerError()
            }
          }
    }
  }

  def databaseCheck(implicit xa: Transactor[IO]) =
    connectionCheck(xa)(timeoutSeconds = Some(4))

  def kafkaCheck(kafkaService: Option[KafkaService]) =
    kafkaService.fold(
      HealthCheck.liftFBoolean(IO.pure(true))
    )(service => HealthCheck.liftFBoolean(service.isHealthy))
}
