package com.hyperplan.infrastructure.serialization.tensorflow

import cats.effect.IO
import cats.implicits._

import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe.{jsonOf, jsonEncoderOf}

import io.circe.parser._
import io.circe.syntax._

import com.hyperplan.domain.models.labels.{
  TensorFlowClassificationLabel,
  TensorFlowClassificationLabels
}

object TensorFlowClassificationLabelsSerializer {

  import io.circe._

  implicit val tfClassificationLabelEncoder
      : Encoder[TensorFlowClassificationLabel] =
    (tfLabel: TensorFlowClassificationLabel) =>
      Json.arr(
        Json.fromString(tfLabel.label),
        Json.fromFloatOrNull(tfLabel.score)
      )

  implicit val tfClassificationLabelDecoder
      : Decoder[TensorFlowClassificationLabel] =
    (c: HCursor) => {
      println("BEFORE VALUES")
      println(c.toString())
      c.downArray.values.foreach(println)
      val values = c.downArray
      println("VALUES BELOW")
      println(values)
      for {
        label <- values.first.as[String]
        score <- values.last.as[Float]
      } yield TensorFlowClassificationLabel(label, score)
    }

  implicit val encoder: Encoder[TensorFlowClassificationLabels] =
    (labels: TensorFlowClassificationLabels) =>
      Json.obj(
        (
          "result",
          Json.arr(
            labels.result.map(
              l =>
                Json
                  .arr(Json.fromString(l.label), Json.fromFloatOrNull(l.score))
            ): _*
          )
        )
      )
  implicit val decoder: Decoder[TensorFlowClassificationLabels] =
    (cursor: HCursor) =>
      cursor.downField("result").as[List[TensorFlowClassificationLabel]].map {
        result =>
          TensorFlowClassificationLabels(result)
      }

  implicit val entityDecoder
      : EntityDecoder[IO, TensorFlowClassificationLabels] =
    jsonOf[IO, TensorFlowClassificationLabels]

  implicit val entityEncoder
      : EntityEncoder[IO, TensorFlowClassificationLabels] =
    jsonEncoderOf[IO, TensorFlowClassificationLabels]

  def encodeJson(project: TensorFlowClassificationLabels): Json = {
    project.asJson
  }

  def decodeJson(n: String): TensorFlowClassificationLabels = {
    decode[TensorFlowClassificationLabels](n).right.get
  }

}
