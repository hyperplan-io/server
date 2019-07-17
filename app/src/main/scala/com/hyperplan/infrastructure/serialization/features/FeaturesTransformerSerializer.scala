package com.hyperplan.infrastructure.serialization.features

import com.hyperplan.domain.models.features.transformers.TensorFlowFeaturesTransformer
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.syntax._
import com.hyperplan.domain.models.features.transformers.RasaNluFeaturesTransformer

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

  val rasaNluTransformerEncoder: Encoder[RasaNluFeaturesTransformer] =
    (transformer: RasaNluFeaturesTransformer) =>
      Json.obj(
        ("field", Json.fromString(transformer.field)),
        ("joinCharacter", Json.fromString(transformer.joinCharacter))
      )

  val rasaNluTransformerDecoder: Decoder[RasaNluFeaturesTransformer] =
    (c: HCursor) =>
      for {
        field <- c.downField("field").as[String]
        joinCharacter <- c.downField("joinCharacter").as[String]
      } yield RasaNluFeaturesTransformer(field, joinCharacter)

}
