package com.foundaml.server.infrastructure.serialization

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.extras.Configuration
import io.circe.parser.decode
import io.circe.syntax._
import com.foundaml.server.domain.models._
import io.circe.generic.extras.semiauto.{deriveDecoder, deriveEncoder}

object AlgorithmPolicySerializer {

  object Implicits {
    implicit val discriminator: Configuration =
      Configuration.default.withDiscriminator("class")

    implicit val encoder: Encoder[AlgorithmPolicy] = deriveEncoder
    implicit val decoder: Decoder[AlgorithmPolicy] = deriveDecoder

  }

  import io.circe.generic.extras.semiauto._
  import Implicits._

  def encodeJsonString(policy: AlgorithmPolicy): String = {
    policy.asJson.noSpaces
  }

  def encodeJson(policy: AlgorithmPolicy): Json = {
    policy.asJson
  }

  def decodeJson(n: String): Either[io.circe.Error, AlgorithmPolicy] = {
    decode[AlgorithmPolicy](n)
  }
}
