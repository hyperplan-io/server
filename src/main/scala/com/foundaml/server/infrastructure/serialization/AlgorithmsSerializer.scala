package com.foundaml.server.infrastructure.serialization

import com.foundaml.server.domain.models.Algorithm
import com.foundaml.server.domain.models.backends.Backend
import io.circe._
import io.circe.syntax._
import io.circe.parser.decode

object AlgorithmsSerializer {

  import io.circe._, io.circe.generic.semiauto._

  object Implicits {
    implicit val backendEncoder: Encoder[Backend] =
      BackendSerializer.Implicits.encoder
    implicit val backendDecoder: Decoder[Backend] =
      BackendSerializer.Implicits.decoder

    implicit val encoder: Encoder[Algorithm] = deriveEncoder
    implicit val decoder: Decoder[Algorithm] = deriveDecoder
  }

  import Implicits._

  def encodeJson(algorithm: Algorithm): Json = {
    algorithm.asJson
  }

  def decodeJson(n: String): Either[io.circe.Error, Algorithm] = {
    decode[Algorithm](n)
  }

}
