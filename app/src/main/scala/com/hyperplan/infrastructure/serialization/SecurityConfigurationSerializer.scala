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

package com.hyperplan.infrastructure.serialization

import com.hyperplan.domain.models._
import com.hyperplan.domain.models.backends.Backend
import io.circe._
import io.circe.syntax._
import io.circe.parser.decode

object SecurityConfigurationSerializer {

  val PLAIN = "plain"
  val AES = "aes"

  implicit val configurationTypeEncoder: Encoder[Encryption] =
    (securityConfigurationType: Encryption) =>
      securityConfigurationType match {
        case PlainEncryption => Json.fromString(PLAIN)
        case AESEncryption => Json.fromString(AES)
      }

  implicit val configurationDecoder: Decoder[Encryption] =
    (c: HCursor) =>
      c.as[String].map {
        case PLAIN => PlainEncryption
        case AES => AESEncryption
      }
  implicit val encoder: Encoder[SecurityConfiguration] =
    (security: SecurityConfiguration) =>
      Json.obj(
        "encryption" -> security.encryption.asJson,
        "headers" -> Json.fromValues {
          security.headers.map {
            case (key, value) =>
              Json.obj(
                "key" -> Json.fromString(key),
                "value" -> Json.fromString(value)
              )
          }
        }
      )

  case class KeyValue(key: String, value: String)
  implicit val decoderKeyValue: Decoder[KeyValue] =
    (c: HCursor) =>
      for {
        key <- c.downField("key").as[String]
        value <- c.downField("value").as[String]
      } yield KeyValue(key, value)

  implicit val decoder: Decoder[SecurityConfiguration] =
    (c: HCursor) =>
      for {
        encryption <- c.downField("encryption").as[Encryption]
        headersKV <- c.downField("headers").as[List[KeyValue]]
        headers = headersKV.map(kv => (kv.key, kv.value))
      } yield PlainSecurityConfiguration(headers)

  def encodeJson(security: SecurityConfiguration): Json = {
    security.asJson
  }

  def encodeJsonNoSpaces(security: SecurityConfiguration): String = {
    security.asJson.noSpaces
  }
  def decodeJson(n: String): Either[io.circe.Error, SecurityConfiguration] = {
    decode[SecurityConfiguration](n)
  }

}
