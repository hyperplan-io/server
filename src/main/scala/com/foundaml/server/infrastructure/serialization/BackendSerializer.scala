package com.foundaml.server.infrastructure.serialization

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

import com.foundaml.server.domain.models.backends.Backend

object BackendSerializer {

  implicit val discriminator: Configuration =
    Configuration.default.withDiscriminator("class")
  implicit val encoder: Encoder[Backend] = implicitly[Encoder[Backend]]
  implicit val decoder: Decoder[Backend] = implicitly[Decoder[Backend]]

  def encodeJson(backend: Backend): String = {
    backend.asJson.noSpaces
  }

  def decodeJson(n: String): Backend = {
    decode[Backend](n).right.get
  }
}
