package com.hyperplan.infrastructure.serialization.labels

import com.hyperplan.domain.models.labels.transformers.TensorFlowLabelsTransformer
import io.circe.{Decoder, Encoder, HCursor, Json}

object LabelsTransformerSerializer {

  val tfLabelsTransformerEncoder: Encoder[TensorFlowLabelsTransformer] =
    (transformer: TensorFlowLabelsTransformer) =>
      Json.obj(
        ("fields", Json.fromFields(transformer.fields.toList.map {
          case (key, value) =>
            key -> Json.fromString(value)
        }))
      )

  val tfLabelsTransformerDecoder: Decoder[TensorFlowLabelsTransformer] =
    (c: HCursor) =>
      for {
        fields <- c.downField("fields").as[Map[String, String]]
      } yield TensorFlowLabelsTransformer(fields)

}
