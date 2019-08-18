package com.hyperplan.infrastructure.serialization.tensorflow

import com.hyperplan.domain.models.features._
import com.hyperplan.domain.models.features.Features._
import io.circe.{Encoder, Decoder, Json}
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf
import io.circe.syntax._
import cats.effect.IO
import cats.implicits._

object BasicFeaturesSerializer {

  implicit val stringMatrixEncoder: Encoder[StringMatrixFeature] = 
    (f: StringMatrixFeature) => Json.obj(f.key -> 
      Json.arr(
        f.data.map(innerData => Json.arr(innerData.map(Json.fromString): _*)): _*
      )
    )
  implicit val stringArrayEncoder: Encoder[StringArrayFeature] = 
    (f: StringArrayFeature) => Json.obj(f.key -> Json.arr(f.data.map(Json.fromString): _*))
  implicit val stringEncoder: Encoder[StringFeature] = 
    (f: StringFeature) => Json.obj(f.key -> Json.fromString(f.value))

  implicit val floatMatrixEncoder: Encoder[FloatMatrixFeature] = 
    (f: FloatMatrixFeature) => Json.obj(f.key -> 
      Json.arr(
        f.data.map(innerData => Json.arr(innerData.map(Json.fromFloatOrNull): _*)): _*
      )
    )
  implicit val floatArrayEncoder: Encoder[FloatArrayFeature] = 
    (f: FloatArrayFeature) => Json.obj(f.key -> Json.arr(f.data.map(Json.fromFloatOrNull): _*))
  implicit val floatEncoder: Encoder[FloatFeature] = 
    (f: FloatFeature) => Json.obj(f.key -> Json.fromFloatOrNull(f.value))

  implicit val intMatrixEncoder: Encoder[IntMatrixFeature] = 
    (f: IntMatrixFeature) => Json.obj(f.key -> 
      Json.arr(
        f.data.map(innerData => Json.arr(innerData.map(Json.fromInt): _*)): _*
      )
    )
  implicit val intArrayEncoder: Encoder[IntArrayFeature] = 
    (f: IntArrayFeature) => Json.obj(f.key -> Json.arr(f.data.map(Json.fromInt): _*))
  implicit val intEncoder: Encoder[IntFeature] = 
    (f: IntFeature) => Json.obj(f.key -> Json.fromInt(f.value))

  implicit val encoder: Encoder[Features] =
    (features: Features) => features.foldLeft(Json.obj()) { 
        case (json: Json, f: StringFeature) => 
          json.deepMerge(stringEncoder(f))
        case (json: Json, f: StringArrayFeature) =>
          json.deepMerge(stringArrayEncoder(f))
        case (json: Json, f: StringMatrixFeature) =>
          json.deepMerge(stringMatrixEncoder(f))
        case (json: Json, f: IntFeature) =>
          json.deepMerge(intEncoder(f))
        case (json: Json, f: IntArrayFeature) =>
          json.deepMerge(intArrayEncoder(f))
        case (json: Json, f: IntMatrixFeature) =>
          json.deepMerge(intMatrixEncoder(f))
        case (json: Json, f: FloatFeature) =>
          json.deepMerge(floatEncoder(f))
        case (json: Json, f: FloatArrayFeature) =>
          json.deepMerge(floatArrayEncoder(f))
        case (json: Json, f: FloatMatrixFeature) =>
          json.deepMerge(floatMatrixEncoder(f))
        case (json: Json, f: ReferenceFeature) =>
          json 
    }
      

  implicit val entityEncoder: EntityEncoder[IO, Features] =
    jsonEncoderOf[IO, Features]

  def encodeJson(features: Features): Json =
    features.asJson

}
