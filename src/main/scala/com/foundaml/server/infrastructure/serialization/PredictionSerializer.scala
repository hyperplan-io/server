package com.foundaml.server.infrastructure.serialization

import io.circe._, io.circe.generic.semiauto._
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

import com.foundaml.server.domain.models.Prediction

object PredictionSerializer {

  implicit val encoder: Encoder[Prediction] = deriveEncoder[Prediction]
  implicit val decoder: Decoder[Prediction] = deriveDecoder[Prediction]

  def encodeJson(request: Prediction): String = {
    request.asJson.noSpaces
  }

  def decodeJson(n: String): Prediction = {
    decode[Prediction](n).right.get
  }
}
