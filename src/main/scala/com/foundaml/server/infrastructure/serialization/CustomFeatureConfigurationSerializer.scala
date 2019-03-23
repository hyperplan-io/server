package com.foundaml.server.infrastructure.serialization

import com.foundaml.server.domain.models.FeatureConfiguration
import io.circe.{Decoder, Encoder}

object CustomFeatureConfigurationSerializer {

  import io.circe.generic.semiauto._

  implicit val encoder: Encoder[FeatureConfiguration] = deriveEncoder
  implicit val decoder: Decoder[FeatureConfiguration] = deriveDecoder
}
