package com.foundaml.server.infrastructure.serialization

import com.foundaml.server.domain.models.Prediction
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

object PredictionSerializer {

  implicit val encoder: Encoder[Prediction] = implicitly[Encoder[Prediction]]
  implicit val decoder: Decoder[Prediction] = implicitly[Decoder[Prediction]]

  def encodeJson(request: Prediction): String = {
    request.asJson.noSpaces
  }

  def decodeJson(n: String): Prediction = {
    decode[Prediction](n).right.get
  }
}
