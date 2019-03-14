package com.foundaml.server.services.infrastructure.serialization

import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

import com.foundaml.server.controllers.requests.PredictionRequest

object PredictionRequestSerializer {

  implicit val encoder: Encoder[PredictionRequest] = implicitly[Encoder[PredictionRequest]]
  implicit val decoder: Decoder[PredictionRequest] = implicitly[Decoder[PredictionRequest]]

  def encodeJson(request: PredictionRequest): String = {
    request.asJson.noSpaces
  }

  def decodeJson(n: String): PredictionRequest = {
    decode[PredictionRequest](n).right.get
  }
}
