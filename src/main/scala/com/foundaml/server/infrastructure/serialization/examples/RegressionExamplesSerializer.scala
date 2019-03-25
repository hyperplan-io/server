package com.foundaml.server.infrastructure.serialization.examples

import com.foundaml.server.domain.models.Examples.RegressionExamples
import com.foundaml.server.domain.models.labels.Label
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._
import io.circe.parser._

object RegressionExamplesSerializer {

  import io.circe.generic.semiauto._

  implicit val labelsEncoder: Encoder[Label] = deriveEncoder
  implicit val labelsDecoder: Decoder[Label] = deriveDecoder

  def encodeJson(examples: RegressionExamples): Json = {
    examples.asJson
  }

  def encodeJsonNoSpaces(examples: RegressionExamples): String = {
    examples.asJson.noSpaces
  }

  def decodeJson(n: String): Either[io.circe.Error, RegressionExamples] = {
    decode[RegressionExamples](n)
  }
}
