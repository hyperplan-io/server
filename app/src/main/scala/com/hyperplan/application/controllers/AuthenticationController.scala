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
import org.http4s._
import org.http4s.headers._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import cats.effect.IO
import cats.implicits._
import com.hyperplan.application.AdminCredentials
import com.hyperplan.application.controllers.requests._
import com.hyperplan.domain.errors._
import com.hyperplan.infrastructure.serialization._
import com.hyperplan.infrastructure.logging.IOLogging
import com.hyperplan.domain._
import com.hyperplan.infrastructure.auth.AuthenticationService._
import com.hyperplan.infrastructure.serialization._
import com.hyperplan.infrastructure.serialization.auth._

import cats.MonadError
import com.hyperplan.infrastructure.auth.AuthenticationService
import com.hyperplan.application.controllers.requests.PostAuthenticationRequest

import com.hyperplan.infrastructure.auth.JwtAuthenticationService
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.http4s.headers.Authorization
import org.http4s.Status
import org.http4s.Response
import org.http4s.Challenge
import cats.data.NonEmptyList

trait AuthenticationController extends Http4sDsl[IO] with IOLogging {

  def generateToken: IO[AuthenticationService.AuthenticationResponse]
  def adminCredentials: AdminCredentials

  val service: HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case req @ POST -> Root =>
        (for {
          request <- req.as[PostAuthenticationRequest](
            MonadError[IO, Throwable],
            PostAuthenticationRequestEntitySerializer.entityDecoder
          )
          correctCredentials = AuthenticationService.validateCredentials(
            request.username,
            request.password,
            adminCredentials
          )
          authResponse <- correctCredentials match {
            case CorrectCredentials =>
              generateToken
            case InCorrectCredentials =>
              IO.raiseError(InvalidCredentials)
          }
        } yield authResponse)
          .flatMap {
            case authResponse =>
              Ok(
                AuthenticationResponseSerializer
                  .encodeJson(authResponse)
              )
          }
          .handleErrorWith {
            case InvalidCredentials =>
              Unauthorized(
                `WWW-Authenticate`(
                  NonEmptyList(
                    Challenge(
                      "Bearer",
                      "Please provide a valid access token"
                    ),
                    Nil
                  )
                ),
                "Authentication failed"
              )
          }
    }
  }
}

class CertificateAuthenticationController(
    publicKey: PublicKey,
    privateKey: PrivateKey,
    issuer: String,
    val adminCredentials: AdminCredentials
) extends AuthenticationController {
  def generateToken =
    JwtAuthenticationService.generateToken(
      AuthenticationData(
        List(AdminScope, PredictionScope),
        issuer,
        Instant.now.plus(1, ChronoUnit.HOURS).some
      ),
      publicKey,
      privateKey
    )
}

class SecretAuthenticationController(
    secret: String,
    issuer: String,
    val adminCredentials: AdminCredentials
) extends AuthenticationController {
  def generateToken =
    JwtAuthenticationService.generateToken(
      AuthenticationData(
        List(AdminScope, PredictionScope),
        issuer,
        Instant.now.plus(1, ChronoUnit.HOURS).some
      ),
      secret
    )
}
