package com.foundaml.server.infrastructure.serialization

import io.circe.parser.decode
import io.circe.syntax._

import com.foundaml.server.domain.models.Prediction

object PredictionSerializer {

  import io.circe._, io.circe.generic.semiauto._

  implicit val encoder: Encoder[Prediction] = deriveEncoder
  implicit val decoder: Decoder[Prediction] = deriveDecoder

  def encodeJson(request: Prediction): String = {
    request.asJson.noSpaces
  }

  def decodeJson(n: String): Prediction = {
    decode[Prediction](n).right.get
  }
}
