package com.hyperplan.infrastructure.serialization.tensorflow

import com.hyperplan.domain.models.labels.TensorFlowRegressionLabels
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe.{jsonOf, jsonEncoderOf}
import io.circe.parser._
import io.circe.syntax._
import cats.effect.IO
import cats.implicits._

object TensorFlowRegressionLabelsSerializer {

  import io.circe._
  import io.circe.generic.semiauto._

  implicit val encoder: Encoder[TensorFlowRegressionLabels] = deriveEncoder
  implicit val decoder: Decoder[TensorFlowRegressionLabels] = deriveDecoder

  implicit val entityDecoder: EntityDecoder[IO, TensorFlowRegressionLabels] =
    jsonOf[IO, TensorFlowRegressionLabels]

  implicit val entityEncoder: EntityEncoder[IO, TensorFlowRegressionLabels] =
    jsonEncoderOf[IO, TensorFlowRegressionLabels]

  def encodeJson(project: TensorFlowRegressionLabels): Json = {
    project.asJson
  }

  def decodeJson(n: String): TensorFlowRegressionLabels = {
    decode[TensorFlowRegressionLabels](n).right.get
  }

}
