package com.hyperplan.infrastructure.serialization

import com.hyperplan.application.controllers.requests.PredictionRequest

import com.hyperplan.domain.models.EntityLink
import com.hyperplan.domain.models.features.Features
import com.hyperplan.domain.models.features.Features.Features

import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

object PredictionRequestSerializer {

  import io.circe.generic.semiauto._

  implicit val entityLinkEncoder: Encoder[EntityLink] =
    PredictionSerializer.entityLinkEncoder
  implicit val entityLinkDecoder: Decoder[EntityLink] =
    PredictionSerializer.entityLinkDecoder

  implicit val encoder: Encoder[PredictionRequest] = deriveEncoder
  implicit val decoder: Decoder[PredictionRequest] = deriveDecoder

  def encodeJson(request: PredictionRequest): String = {
    request.asJson.noSpaces
  }

  def decodeJson(n: String): PredictionRequest = {
    decode[PredictionRequest](n).right.get
  }
}
