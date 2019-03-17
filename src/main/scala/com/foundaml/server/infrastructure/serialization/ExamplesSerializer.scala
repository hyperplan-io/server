package com.foundaml.server.infrastructure.serialization

import com.foundaml.server.domain.models.Examples
import com.foundaml.server.domain.models.labels.Label
import io.circe._
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

object ExamplesSerializer {

  import io.circe.generic.semiauto._

  implicit val labelsEncoder: Encoder[Label] = deriveEncoder
  implicit val labelsDecoder: Decoder[Label] = deriveDecoder

  implicit val encoder: Encoder[Examples] = deriveEncoder
  implicit val decoder: Decoder[Examples] = deriveDecoder

  def encodeJson(examples: Examples): Json = {
    examples.asJson
  }

  def encodeJsonNoSpaces(examples: Examples): String = {
    examples.asJson.noSpaces
  }

  def decodeJson(n: String): Examples = {
    decode[Examples](n).right.get
  }
}
