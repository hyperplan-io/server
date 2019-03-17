package com.foundaml.server.infrastructure.serialization

import io.circe.parser.decode
import io.circe.syntax._
import com.foundaml.server.domain.models.{Examples, Prediction}
import com.foundaml.server.domain.models.features.Features
import com.foundaml.server.domain.models.labels.Labels

object PredictionSerializer {

  import io.circe._, io.circe.generic.semiauto._

  implicit val featuresEncoder: Encoder[Features] =
    FeaturesSerializer.Implicits.encoder
  implicit val featuresDecoder: Decoder[Features] =
    FeaturesSerializer.Implicits.decoder

  implicit val labelsEncoder: Encoder[Labels] = LabelsSerializer.encoder
  implicit val labelsDecoder: Decoder[Labels] = LabelsSerializer.decoder

  implicit val examplesEncoder: Encoder[Examples] = ExamplesSerializer.encoder
  implicit val examplesDecoder: Decoder[Examples] = ExamplesSerializer.decoder

  implicit val encoder: Encoder[Prediction] = deriveEncoder
  implicit val decoder: Decoder[Prediction] = deriveDecoder

  def encodeJson(request: Prediction): Json = {
    request.asJson
  }

  def decodeJson(n: String): Prediction = {
    decode[Prediction](n).right.get
  }
}
