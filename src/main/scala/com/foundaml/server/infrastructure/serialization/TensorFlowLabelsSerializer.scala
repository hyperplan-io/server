package com.foundaml.server.infrastructure.serialization

import com.foundaml.server.domain.models.labels.transformers.TensorFlowLabels
import io.circe.syntax._
import org.http4s.circe.jsonOf
import io.circe.{Encoder, _}
import io.circe.parser.decode
import org.http4s.EntityDecoder
import scalaz.zio.Task
import scalaz.zio.interop.catz._

object TensorFlowLabelsSerializer {

  import io.circe._, io.circe.generic.semiauto._

  implicit val encoder: Encoder[TensorFlowLabels] = deriveEncoder
  implicit val decoder: Decoder[TensorFlowLabels] = deriveDecoder

  implicit val entityDecoder: EntityDecoder[Task, TensorFlowLabels] =
    jsonOf[Task, TensorFlowLabels]

  def encodeJson(project: TensorFlowLabels): Json = {
    project.asJson
  }

  def decodeJson(n: String): TensorFlowLabels = {
    decode[TensorFlowLabels](n).right.get
  }

}
