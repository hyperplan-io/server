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
import com.hyperplan.domain.models.errors.{
  AlgorithmAlreadyExists,
  IncompatibleFeatures,
  IncompatibleLabels
}
import com.hyperplan.domain.services.AlgorithmsService
import com.hyperplan.infrastructure.serialization._
import com.hyperplan.infrastructure.logging.IOLogging
import com.hyperplan.domain._
import com.hyperplan.infrastructure.auth.AuthenticationService._
import com.hyperplan.infrastructure.serialization._
import com.hyperplan.infrastructure.serialization.auth._

class AuthenticationController(
    adminCredentials: AdminCredentials,
    publicKey: PublicKey,
    privateKey: PrivateKey
) extends Http4sDsl[IO]
    with IOLogging {

  import cats.MonadError
  import com.hyperplan.infrastructure.auth.AuthenticationService
  import com.foundaml.server.controllers.requests.PostAuthenticationRequest

  import com.hyperplan.infrastructure.auth.JwtAuthenticationService
  import java.time.Instant
  import java.time.temporal.ChronoUnit
  import org.http4s.headers.Authorization
  import org.http4s.Status
  import org.http4s.Response
  import org.http4s.Challenge
  import cats.data.NonEmptyList
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
              JwtAuthenticationService.generateToken(
                AuthenticationData(
                  List(AdminScope, PredictionScope),
                  "foundaml",
                  Instant.now.plus(1, ChronoUnit.HOURS).some
                ),
                publicKey,
                privateKey
              )
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
