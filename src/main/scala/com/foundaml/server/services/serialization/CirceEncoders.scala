package com.foundaml.server.services.serialization

import io.circe._, io.circe.generic.semiauto._

import com.foundaml.server.models._

object CirceEncoders {

  implicit val predictionDecoder: Decoder[Prediction] = deriveDecoder[Prediction]
	implicit val predictionEncoder: Encoder[Prediction] = deriveEncoder[Prediction]
}
