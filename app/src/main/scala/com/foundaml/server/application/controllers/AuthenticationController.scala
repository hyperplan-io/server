package com.foundaml.server.application.controllers

import cats.Functor
import org.http4s.{HttpRoutes, HttpService}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import cats.effect.IO
import cats.implicits._
import com.foundaml.server.application.controllers.requests._
import com.foundaml.server.domain.models.errors.{
  AlgorithmAlreadyExists,
  IncompatibleFeatures,
  IncompatibleLabels
}
import com.foundaml.server.domain.services.AlgorithmsService
import com.foundaml.server.infrastructure.serialization._
import com.foundaml.server.infrastructure.logging.IOLogging
import com.foundaml.server.domain._
import com.foundaml.server.infrastructure.auth.AuthenticationService._

import com.foundaml.server.infrastructure.serialization._
import com.foundaml.server.infrastructure.serialization.auth._
class AuthenticationController(
    adminCredentials: AdminCredentials,
    publicKey: PublicKey,
    privateKey: PrivateKey
) extends Http4sDsl[IO]
    with IOLogging {

  import cats.MonadError
  import com.foundaml.server.infrastructure.auth.AuthenticationService
  import com.foundaml.server.controllers.requests.PostAuthenticationRequest

  import com.foundaml.server.infrastructure.auth.JwtAuthenticationService
  import java.time.Instant
  import java.time.temporal.ChronoUnit
  import org.http4s.headers.Authorization
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
          token <- correctCredentials match {
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
        } yield token)
          .flatMap {
            case token =>
              Ok(
                AuthenticationResponseSerializer
                  .encodeJson(AuthenticationResponse(token))
              )
          }
          .handleErrorWith {
            case InvalidCredentials =>
              BadRequest("")
          }
    }
  }

}
