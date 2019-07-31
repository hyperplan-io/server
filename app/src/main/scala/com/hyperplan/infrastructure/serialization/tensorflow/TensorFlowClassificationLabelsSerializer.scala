package com.hyperplan.infrastructure.serialization.tensorflow

import cats.effect.IO
import cats.implicits._

import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import io.circe.parser._
import io.circe.syntax._

import com.hyperplan.domain.models.labels.{
  TensorFlowClassificationLabel,
  TensorFlowClassificationLabels
}

object TensorFlowClassificationLabelsSerializer {

  import io.circe._
  import io.circe.generic.semiauto._

  implicit val tfClassificationLabelEncoder
      : Encoder[TensorFlowClassificationLabel] =
    (tfLabel: TensorFlowClassificationLabel) =>
      Json.arr(
        Json.fromString(tfLabel.label),
        Json.fromFloatOrNull(tfLabel.score)
      )
  implicit val tfClassificationLabelDecoder
      : Decoder[TensorFlowClassificationLabel] =
    (c: HCursor) =>
      for {
        values <- c.as[List[String]]
        label = values.head
        score = values.tail.head.toFloat
      } yield TensorFlowClassificationLabel(label, score)

  implicit val encoder: Encoder[TensorFlowClassificationLabels] = deriveEncoder
  implicit val decoder: Decoder[TensorFlowClassificationLabels] =
    (cursor: HCursor) =>
      cursor.downField("result").as[List[TensorFlowClassificationLabel]].map {
        result =>
          TensorFlowClassificationLabels(result)
      }

  implicit val entityDecoder
      : EntityDecoder[IO, TensorFlowClassificationLabels] =
    jsonOf[IO, TensorFlowClassificationLabels]

  def encodeJson(project: TensorFlowClassificationLabels): Json = {
    project.asJson
  }

  def decodeJson(n: String): TensorFlowClassificationLabels = {
    decode[TensorFlowClassificationLabels](n).right.get
  }

}
