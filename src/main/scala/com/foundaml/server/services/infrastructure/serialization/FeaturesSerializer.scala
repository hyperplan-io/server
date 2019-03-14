package com.foundaml.server.services.infrastructure.serialization

import com.foundaml.server.models.features._

import io.circe.{Encoder, Json}
import io.circe._
import io.circe.syntax._

object FeaturesSerializer {

  implicit val encodeTFClassificationFeatures: Encoder[TensorFlowClassificationFeatures] = (features: TensorFlowClassificationFeatures) => Json.obj(
    ("signature_name", Json.fromString(features.signatureName)),
    ("examples", Json.arr(Json.fromFields(features.examples.flatMap {
      case TensorFlowDoubleFeature(key, value) =>
        Json.fromDouble(value).map(json => key -> json)
      case TensorFlowFloatFeature(key, value) =>
        Json.fromFloat(value).map(json => key -> json)
      case TensorFlowIntFeature(key, value) =>
        Some(key -> Json.fromInt(value))
      case TensorFlowStringFeature(key, value) =>
        Some(key -> Json.fromString(value))
    }
    )))
  )

  def encodeJson(features: TensorFlowClassificationFeatures): Json = features.asJson

}
