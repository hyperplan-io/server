package com.foundaml.server.infrastructure.serialization.features

import com.foundaml.server.domain.models.features.transformers.TensorFlowFeaturesTransformer
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.syntax._

object FeaturesTransformerSerializer {

  val tfTransformerEncoder: Encoder[TensorFlowFeaturesTransformer] =
    (transformer: TensorFlowFeaturesTransformer) =>
      Json.obj(
        ("signatureName", Json.fromString(transformer.signatureName)),
        (
          "mapping",
          transformer.fields
            .map(keyValue => keyValue._1 -> Json.fromString(keyValue._2))
            .asJson
        )
      )

  val tfTransformerDecoder: Decoder[TensorFlowFeaturesTransformer] =
    (c: HCursor) =>
      for {
        signatureName <- c.downField("signatureName").as[String]
        fields <- c.downField("mapping").as[Map[String, String]]
      } yield TensorFlowFeaturesTransformer(signatureName, fields)

}
