package com.foundaml.server.infrastructure.serialization.features

import com.foundaml.server.domain.models.features.transformers.TensorFlowFeaturesTransformer
import io.circe.{Decoder, Encoder, HCursor, Json}

object FeaturesTransformerSerializer {

  val tfTransformerEncoder: Encoder[TensorFlowFeaturesTransformer] =
    (transformer: TensorFlowFeaturesTransformer) =>
      Json.obj(
        ("signatureName", Json.fromString(transformer.signatureName)),
        ("fields", Json.fromValues(transformer.fields.map(Json.fromString)))
      )

  val tfTransformerDecoder: Decoder[TensorFlowFeaturesTransformer] =
    (c: HCursor) =>
      for {
        signatureName <- c.downField("signatureName").as[String]
        fields <- c.downField("fields").as[Set[String]]
      } yield TensorFlowFeaturesTransformer(signatureName, fields)

}
