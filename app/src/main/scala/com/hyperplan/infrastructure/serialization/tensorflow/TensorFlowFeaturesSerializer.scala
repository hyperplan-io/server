package com.hyperplan.infrastructure.serialization.tensorflow

import com.hyperplan.domain.models.features._
import io.circe.{Encoder, Json}
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf
import io.circe.syntax._
import cats.effect.IO
import cats.implicits._

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
          case TensorFlowEmptyVectorFeature(key) =>
            Some(key -> Json.fromValues(Nil))
          case TensorFlowIntVector2dFeature(key, values2d) =>
            Some(key -> Json.fromValues(values2d.map { v =>
              Json.fromValues(v.map(Json.fromInt))
            }))
          case TensorFlowFloatVector2dFeature(key, values2d) =>
            Some(key -> Json.fromValues(values2d.map { v =>
              Json.fromValues(v.map(Json.fromFloatOrNull))
            }))
          case TensorFlowStringVector2dFeature(key, values2d) =>
            Some(key -> Json.fromValues(values2d.map { v =>
              Json.fromValues(v.map(Json.fromString))
            }))

        })))
      )

  implicit val entityEncoder: EntityEncoder[IO, TensorFlowFeatures] =
    jsonEncoderOf[IO, TensorFlowFeatures]

  def encodeJson(features: TensorFlowFeatures): Json =
    features.asJson

}
