package com.foundaml.server.models.features.transformers

import io.circe._
import io.circe.syntax._

import com.foundaml.server.models.features._

class TensorFlowFeaturesTransformer(signatureName: String, fields: List[String]) {


  implicit val encodeTFClassificationFeatures: Encoder[TensorFlowClassificationFeatures] = new Encoder[TensorFlowClassificationFeatures] {

    final def apply(features: TensorFlowClassificationFeatures): Json = Json.obj(
      ("signature_name", Json.fromString(features.signatureName)),
      ("examples", Json.arr(Json.fromFields(features.examples.flatMap{
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
  }

  def transform(features: Features): Either[Throwable, TensorFlowClassificationFeatures] =  {
    if (features.features.length == fields.length) {
      val examples = features.features.zip(fields).map {
        case (DoubleFeature(value), field) =>
          TensorFlowDoubleFeature(field, value)
        case (FloatFeature(value), field) =>
          TensorFlowFloatFeature(field, value)
        case (IntFeature(value), field) =>
          TensorFlowIntFeature(field, value)
        case (StringFeature(value), field) =>
          TensorFlowStringFeature(field, value)
      }
      Right(TensorFlowClassificationFeatures(signatureName, examples))
    } else {
      Left(new IllegalArgumentException(""))
    }
  } 

  def toJson(tfFeatures: TensorFlowClassificationFeatures) = tfFeatures.asJson
}
