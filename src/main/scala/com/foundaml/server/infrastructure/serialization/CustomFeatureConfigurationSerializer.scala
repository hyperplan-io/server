package com.foundaml.server.infrastructure.serialization

import com.foundaml.server.domain.models.CustomFeatureConfiguration
import io.circe.{Decoder, Encoder}

object CustomFeatureConfigurationSerializer {

  import io.circe.generic.semiauto._

  implicit val encoder: Encoder[CustomFeatureConfiguration] = deriveEncoder
  implicit val decoder: Decoder[CustomFeatureConfiguration] = deriveDecoder
}
