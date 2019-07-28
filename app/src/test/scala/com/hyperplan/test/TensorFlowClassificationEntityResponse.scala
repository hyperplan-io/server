package com.hyperplan.test

import cats.effect.IO
import io.circe.{Decoder, Encoder, HCursor, Json}
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

case class TensorFlowClassificationResult(label: String, score: String)
object TensorFlowClassificationResult {
  implicit val encoder: Encoder[TensorFlowClassificationResult] =
    (result: TensorFlowClassificationResult) =>
      Json.arr(
        Json.fromString(result.label),
        Json.fromString(result.score)
      )
  implicit val decodeR: Decoder[TensorFlowClassificationResult] =
    (c: HCursor) =>
      c.downArray.as[List[String]].map { values =>
        TensorFlowClassificationResult(
          values.head,
          values.tail.head
        )
      }
}
case class TensorFlowClassificationEntityResponse(
    result: List[TensorFlowClassificationResult]
)

object TensorFlowClassificationEntityResponse {
  implicit val encoder: Encoder[TensorFlowClassificationEntityResponse] =
    (response: TensorFlowClassificationEntityResponse) =>
      Json.obj(
        "result" -> Json.arr(
          response.result.map(TensorFlowClassificationResult.encoder.apply): _*
        )
      )
  implicit val decoder: Decoder[TensorFlowClassificationEntityResponse] =
    (cursor: HCursor) =>
      cursor.downField("result").as[List[TensorFlowClassificationResult]].map {
        result =>
          TensorFlowClassificationEntityResponse(result)
      }
  implicit val decoderList: Decoder[List[TensorFlowClassificationResult]] =
    Decoder.decodeList[TensorFlowClassificationResult]
  implicit val entityEncoder
      : EntityEncoder[IO, TensorFlowClassificationEntityResponse] =
    jsonEncoderOf[IO, TensorFlowClassificationEntityResponse]

}

case class TensorFlowRegressionResult(score: Float)
object TensorFlowRegressionResult {
  implicit val encoder: Encoder[TensorFlowRegressionResult] =
    (result: TensorFlowRegressionResult) =>
      Json.arr(
        Json.fromFloatOrNull(result.score)
      )
  implicit val decoder: Decoder[TensorFlowRegressionResult] =
    (c: HCursor) =>
      c.downArray.as[List[Float]].map { values =>
        TensorFlowRegressionResult(
          values.head
        )
      }
}
case class TensorFlowRegressionEntityResponse(
    result: List[TensorFlowRegressionResult]
)

object TensorFlowRegressionEntityResponse {
  implicit val encoder: Encoder[TensorFlowRegressionEntityResponse] =
    (response: TensorFlowRegressionEntityResponse) =>
      Json.obj(
        "result" -> Json.arr(
          response.result.map(TensorFlowRegressionResult.encoder.apply): _*
        )
      )
  implicit val decoder: Decoder[TensorFlowRegressionEntityResponse] =
    (cursor: HCursor) =>
      cursor.downField("result").as[List[TensorFlowRegressionResult]].map {
        result =>
          TensorFlowRegressionEntityResponse(result)
      }
  implicit val decoderList: Decoder[List[TensorFlowRegressionResult]] =
    Decoder.decodeList[TensorFlowRegressionResult]
  implicit val entityEncoder
      : EntityEncoder[IO, TensorFlowRegressionEntityResponse] =
    jsonEncoderOf[IO, TensorFlowRegressionEntityResponse]

}
