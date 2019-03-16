package com.foundaml.server.infrastructure.serialization

import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import com.foundaml.server.application.controllers.requests.PredictionRequest
import com.foundaml.server.domain.models.features.{Feature, Features}

object PredictionRequestSerializer {

  import io.circe.generic.extras.semiauto._
  import FeaturesSerializer.Implicits._

  implicit val encoder2: Encoder[Features] = FeaturesSerializer.encoder
  implicit val decoder2: Decoder[Features] = FeaturesSerializer.decoder

  implicit val encoder: Encoder[PredictionRequest] = deriveEncoder
  implicit val decoder: Decoder[PredictionRequest] = deriveDecoder

  def encodeJson(request: PredictionRequest): String = {
    request.asJson.noSpaces
  }

  def decodeJson(n: String): PredictionRequest = {
    decode[PredictionRequest](n).right.get
  }
}
