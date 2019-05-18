package com.foundaml.server.infrastructure.serialization.auth

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.extras.Configuration
import io.circe.parser.decode
import io.circe.syntax._
import com.foundaml.server.domain.models._
import com.foundaml.server.infrastructure.auth.AuthenticationService

object AuthenticationResponseSerializer {

  implicit val encoder
      : Encoder[AuthenticationService.AuthenticationResponse] = {
    (response: AuthenticationService.AuthenticationResponse) =>
      Json.obj(
        "token" -> Json.fromString(response.token)
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
