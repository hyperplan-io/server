package com.foundaml.server.infrastructure.serialization

import io.circe.generic.extras.Configuration
import io.circe._
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import com.foundaml.server.domain.models.backends.Backend
import com.foundaml.server.domain.models.features.transformers.TensorFlowFeaturesTransformer
import com.foundaml.server.domain.models.labels.Labels
import com.foundaml.server.domain.models.labels.transformers.{TensorFlowLabel, TensorFlowLabelsTransformer}

object BackendSerializer {

  import io.circe.generic.extras.semiauto._

  implicit val discriminator: Configuration =
    Configuration.default.withDiscriminator("class")

  implicit val encoder: Encoder[Backend] = deriveEncoder[Backend]
  implicit val decoder: Decoder[Backend] = deriveDecoder[Backend]

  implicit val ftTransformerEncoder: Encoder[TensorFlowFeaturesTransformer] = deriveEncoder[TensorFlowFeaturesTransformer]
  implicit val ftTransformerDecoder: Decoder[TensorFlowFeaturesTransformer] = deriveDecoder[TensorFlowFeaturesTransformer]

  implicit val labelsTransformerEncoder: Encoder[TensorFlowLabelsTransformer] = deriveEncoder[TensorFlowLabelsTransformer]
  implicit val labelsTransformerDecoder: Decoder[TensorFlowLabelsTransformer] = deriveDecoder[TensorFlowLabelsTransformer]

  implicit val tfLabelTransformerEncoder: Encoder[TensorFlowLabel] = deriveEncoder[TensorFlowLabel]
  implicit val tfLabelTransformerDecoder: Decoder[TensorFlowLabel] = deriveDecoder[TensorFlowLabel]

  implicit val labelsEncoder: Encoder[Labels] = LabelsSerializer.encoder
  implicit val labelsDecoder: Decoder[Labels] = LabelsSerializer.decoder

  def encodeJson(backend: Backend): String = {
    backend.asJson.noSpaces
  }

  def decodeJson(n: String): Backend = {
    decode[Backend](n).right.get
  }
}
