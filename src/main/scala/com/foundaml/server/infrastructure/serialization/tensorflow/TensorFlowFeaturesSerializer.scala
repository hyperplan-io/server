package com.foundaml.server.infrastructure.serialization.tensorflow

import com.foundaml.server.domain.models.features._
import io.circe.{Encoder, Json}
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf
import io.circe.syntax._
import scalaz.zio.Task
import scalaz.zio.interop.catz._

object TensorFlowFeaturesSerializer {

  implicit val encodeTFClassificationFeatures: Encoder[TensorFlowFeatures] =
    (features: TensorFlowFeatures) =>
      Json.obj(
        ("signature_name", Json.fromString(features.signatureName)),
        ("examples", Json.arr(Json.fromFields(features.examples.flatMap {
          case TensorFlowFloatFeature(key, value) =>
            Json.fromDouble(value).map(json => key -> json)
          case TensorFlowIntFeature(key, value) =>
            Some(key -> Json.fromInt(value))
          case TensorFlowStringFeature(key, value) =>
            Some(key -> Json.fromString(value))
          case TensorFlowFloatVectorFeature(key, values) =>
            Some(key -> Json.fromValues(values.flatMap(Json.fromFloat)))
          case TensorFlowIntVectorFeature(key, values) =>
            Some(key -> Json.fromValues(values.map(Json.fromInt)))
          case TensorFlowStringVectorFeature(key, values) =>
            Some(key -> Json.fromValues(values.map(Json.fromString)))
        })))
      )

  implicit val entityEncoder: EntityEncoder[Task, TensorFlowFeatures] =
    jsonEncoderOf[Task, TensorFlowFeatures]

  def encodeJson(features: TensorFlowFeatures): Json =
    features.asJson

}
