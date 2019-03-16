package com.foundaml.server.infrastructure.serialization

import com.foundaml.server.domain.models.Algorithm
import com.foundaml.server.domain.models.backends.Backend
import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

object AlgorithmsSerializer {

  object Implicits {
    implicit val backendEncoder: Encoder[Backend] =
      BackendSerializer.Implicits.encoder
    implicit val backendDecoder: Decoder[Backend] =
      BackendSerializer.Implicits.decoder

    implicit val encoder: Encoder[Algorithm] = deriveEncoder
    implicit val decoder: Decoder[Algorithm] = deriveDecoder
  }

  import io.circe._, io.circe.generic.semiauto._
  import Implicits._

}
