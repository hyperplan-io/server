package com.hyperplan.infrastructure.serialization.tensorflow

import com.hyperplan.domain.models.labels.TensorFlowRegressionLabels
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
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

  def encodeJson(project: TensorFlowRegressionLabels): Json = {
    project.asJson
  }

  def decodeJson(n: String): TensorFlowRegressionLabels = {
    decode[TensorFlowRegressionLabels](n).right.get
  }

}
