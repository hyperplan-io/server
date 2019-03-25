package com.foundaml.server.infrastructure.serialization.examples

import com.foundaml.server.domain.models.Examples.ClassificationExamples
import com.foundaml.server.domain.models.labels.Label
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._
import io.circe.parser._

object ClassificationExamplesSerializer {

  import io.circe.generic.semiauto._

  implicit val labelsEncoder: Encoder[Label] = deriveEncoder
  implicit val labelsDecoder: Decoder[Label] = deriveDecoder

  def encodeJson(examples: ClassificationExamples): Json = {
    examples.asJson
  }

  def encodeJsonNoSpaces(examples: ClassificationExamples): String = {
    examples.asJson.noSpaces
  }

  def decodeJson(n: String): Either[io.circe.Error, ClassificationExamples] = {
    decode[ClassificationExamples](n)
  }
}
