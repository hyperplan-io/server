package com.foundaml.server.infrastructure.serialization

import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import com.foundaml.server.application.controllers.requests.PredictionRequest
import com.foundaml.server.domain.models.features.{Feature, Features}

object PredictionRequestSerializer {

  import io.circe.generic.semiauto._

  implicit val featuresEncoder: Encoder[Features] =
    FeaturesSerializer.Implicits.encoder
  implicit val featuresDecoder: Decoder[Features] =
    FeaturesSerializer.Implicits.decoder

  implicit val encoder: Encoder[PredictionRequest] = deriveEncoder
  implicit val decoder: Decoder[PredictionRequest] = deriveDecoder

  def encodeJson(request: PredictionRequest): String = {
    request.asJson.noSpaces
  }

  def decodeJson(n: String): PredictionRequest = {
    decode[PredictionRequest](n).right.get
  }
}
