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
