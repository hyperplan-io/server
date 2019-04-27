package com.foundaml.server.infrastructure.serialization.tensorflow

import com.foundaml.server.domain.models.labels.TensorFlowClassificationLabels
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import io.circe.parser._
import io.circe.syntax._
import cats.effect.IO
import cats.implicits._

object TensorFlowClassificationLabelsSerializer {

  import io.circe._
  import io.circe.generic.semiauto._

  implicit val encoder: Encoder[TensorFlowClassificationLabels] = deriveEncoder
  implicit val decoder: Decoder[TensorFlowClassificationLabels] = deriveDecoder

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
