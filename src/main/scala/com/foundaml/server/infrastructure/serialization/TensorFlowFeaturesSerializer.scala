package com.foundaml.server.infrastructure.serialization

import com.foundaml.server.domain.models.features._
import io.circe.{Encoder, _}
import io.circe.generic.extras.Configuration
import io.circe.parser.decode
import io.circe.syntax._

object TensorFlowFeaturesSerializer {

  import io.circe.generic.extras.semiauto._

  implicit val encodeTFClassificationFeatures
      : Encoder[TensorFlowClassificationFeatures] =
    (features: TensorFlowClassificationFeatures) =>
      Json.obj(
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
        })))
      )

  def encodeJson(features: TensorFlowClassificationFeatures): Json =
    features.asJson

}
