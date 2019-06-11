package com.hyperplan.infrastructure.serialization.auth

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.extras.Configuration
import io.circe.parser.decode
import io.circe.syntax._
import com.hyperplan.domain.models._
import com.hyperplan.infrastructure.auth.AuthenticationService

object AuthenticationScopeSerializer {

  implicit val encoder: Encoder[AuthenticationService.AuthenticationScope] = {
    (scope: AuthenticationService.AuthenticationScope) =>
      Json.fromString(scope.scope)
  }

  implicit val encoderList
      : Encoder[List[AuthenticationService.AuthenticationScope]] =
    (a: List[AuthenticationService.AuthenticationScope]) =>
      Json.fromValues(a.map(encoder.apply))

  implicit val decoder: Decoder[AuthenticationService.AuthenticationScope] =
    (c: HCursor) =>
      c.as[String].flatMap {
        case AuthenticationService.AdminScope.scope =>
          Right(AuthenticationService.AdminScope)
        case AuthenticationService.PredictionScope.scope =>
          Right(AuthenticationService.PredictionScope)
      }

  implicit val decoderList
      : Decoder[List[AuthenticationService.AuthenticationScope]] =
    (c: HCursor) =>
      Decoder.decodeList[AuthenticationService.AuthenticationScope](decoder)(c)

  def encodeJsonString(
      scope: AuthenticationService.AuthenticationScope
  ): String = {
    scope.asJson.noSpaces
  }

  def encodeJsonListString(
      scope: List[AuthenticationService.AuthenticationScope]
  ): String = {
    scope.asJson.noSpaces
  }
  def encodeJson(scope: AuthenticationService.AuthenticationScope): Json = {
    scope.asJson
  }

  def encodeJsonList(
      scope: List[AuthenticationService.AuthenticationScope]
  ): Json = {
    scope.asJson
  }
  def decodeJson(
      n: String
  ): Either[io.circe.Error, AuthenticationService.AuthenticationScope] = {
    decode[AuthenticationService.AuthenticationScope](n)
  }

  def decodeJsonList(
      n: String
  ): Either[io.circe.Error, List[AuthenticationService.AuthenticationScope]] = {
    decode[List[AuthenticationService.AuthenticationScope]](n)
  }
}
