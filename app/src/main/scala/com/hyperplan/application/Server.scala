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

package com.hyperplan.application

import cats.effect.Timer
import cats.effect.IO
import cats.implicits._

import doobie._

import org.http4s.server.blaze.{BlazeBuilder, BlazeServerBuilder}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, NANOSECONDS, TimeUnit}
import scala.util.Properties.envOrNone
import com.hyperplan.application.controllers._
import com.hyperplan.domain.repositories.ProjectsRepository

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
      publicKeyOpt: Option[AuthenticationService.PublicKey],
      privateKeyOpt: Option[AuthenticationService.PrivateKey],
      secretOpt: Option[String]
  ) = {
    val predictionsController = new PredictionsController(
      projectsService,
      domainService,
      predictionsService
    )
    val projectsController = new ProjectsController(
      projectsService
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
    val privacyController = new PrivacyController(
      privacyService
    )
    val healthController = new HealthController(
      xa,
      kafkaService
    )

    val eitherAuth: Either[
      Throwable,
      (
          (
              HttpRoutes[IO],
              AuthenticationService.AuthenticationScope
          ) => HttpRoutes[IO],
          AuthenticationController
      )
    ] = (publicKeyOpt, privateKeyOpt, secretOpt) match {
      case (Some(publicKey), Some(privateKey), None) =>
        Right(
          AuthenticationMiddleware
            .jwtAuthenticateWithCertificate(publicKey, privateKey),
          new CertificateAuthenticationController(
            publicKey,
            privateKey,
            config.encryption.issuer,
            config.credentials
          )
        )
      case (None, None, Some(secret)) =>
        Right(
          AuthenticationMiddleware.jwtAuthenticateWithSecret(secret),
          new SecretAuthenticationController(
            secret,
            config.encryption.issuer,
            config.credentials
          )
        )
      case _ =>
        Left(
          new Exception(
            "You need to either set a secret with APP_SECRET environment variable or configure a certificate with PUBLIC_KEY and PRIVATE_KEY environment variables"
          )
        )
    }

    eitherAuth.map {
      case (authMiddleware, authController) =>
        BlazeServerBuilder[IO]
          .bindHttp(port, "0.0.0.0")
          .withHttpApp(
            Router(
              "/predictions" -> (
                if (config.security.protectPredictionRoute) {
                  authMiddleware(
                    predictionsController.service,
                    AuthenticationService.PredictionScope
                  )
                } else {
                  predictionsController.service
                }
              ),
              "/projects" -> (
                authMiddleware(
                  projectsController.service,
                  AuthenticationService.AdminScope
                )
              ),
              "/examples" -> (
                authMiddleware(
                  examplesController.service,
                  AuthenticationService.PredictionScope
                )
              ),
              "/features" -> (
                authMiddleware(
                  featuresController.service,
                  AuthenticationService.AdminScope
                )
              ),
              "/labels" -> (
                authMiddleware(
                  labelsController.service,
                  AuthenticationService.AdminScope
                )
              ),
              "/privacy" -> (
                authMiddleware(
                  privacyController.service,
                  AuthenticationService.AdminScope
                )
              ),
              "/authentication" -> (
                authController.service
              ),
              "/_health" -> (
                healthController.service
              )
            ).orNotFound
          )
          .serve
    }

  }

}
