package com.foundaml.server.services.infrastructure.serialization

import com.foundaml.server.models._
import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.auto._
import io.circe.generic.extras.Configuration
import io.circe.parser.decode
import io.circe.syntax._

object AlgorithmPolicySerializer {

  implicit val discriminator: Configuration = Configuration.default.withDiscriminator("class")

  implicit val encoder: Encoder[AlgorithmPolicy] = implicitly[Encoder[AlgorithmPolicy]]
  implicit val decoder: Decoder[AlgorithmPolicy] = implicitly[Decoder[AlgorithmPolicy]]

  def encodeJson(policy: AlgorithmPolicy): String = {
    policy.asJson.noSpaces
  }

  def decodeJson(n: String): Either[io.circe.Error, AlgorithmPolicy] = {
    decode[AlgorithmPolicy](n)
  }
}
