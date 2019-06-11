package com.hyperplan.infrastructure.serialization.auth

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.extras.Configuration
import io.circe.parser.decode
import io.circe.syntax._
import com.hyperplan.domain.models._
import com.hyperplan.infrastructure.auth.AuthenticationService

object AuthenticationResponseSerializer {

  implicit val encoder
      : Encoder[AuthenticationService.AuthenticationResponse] = {
    (response: AuthenticationService.AuthenticationResponse) =>
      Json.obj(
        "token" -> Json.fromString(response.token),
        "scope" -> Json.fromValues(
          response.scope.map(scope => Json.fromString(scope.scope))
        )
      )
  }

  def encodeJsonString(
      response: AuthenticationService.AuthenticationResponse
  ): String = {
    response.asJson.noSpaces
  }

  def encodeJson(
      response: AuthenticationService.AuthenticationResponse
  ): Json = {
    response.asJson
  }

}
