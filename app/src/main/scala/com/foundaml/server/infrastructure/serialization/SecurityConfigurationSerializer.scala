package com.foundaml.server.infrastructure.serialization

import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.backends.Backend
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
        "headers" -> Json.fromValues{security.headers.map { case (key, value) =>
          Json.obj(
            "key" -> Json.fromString(key),
            "value" -> Json.fromString(value)
          )
        }},
      )

  implicit val decoder: Decoder[SecurityConfiguration] =
    (c: HCursor) =>
      for {
        encryption <- c.downField("encryption").as[Encryption]
        headers <- c.downField("headers").as[List[(String,String)]]
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
