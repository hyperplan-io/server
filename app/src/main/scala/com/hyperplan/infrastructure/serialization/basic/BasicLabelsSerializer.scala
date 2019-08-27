package com.hyperplan.infrastructure.serialization.rasa

import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import io.circe.parser._
import io.circe.syntax._

import cats.effect.IO
import cats.implicits._
import com.hyperplan.domain.models.labels._
import com.hyperplan.domain.models.labels.BasicLabel._

object BasicLabelsSerializer {

  import io.circe._
  import io.circe.generic.semiauto._

  implicit val basicLabelEncoder: Encoder[BasicLabel] = deriveEncoder
  implicit val basicLabelDecoder: Decoder[BasicLabel] = deriveDecoder

  implicit val encoder: Encoder[BasicLabels] =
    Encoder.encodeList[BasicLabel]
  implicit val decoder: Decoder[BasicLabels] =
    Decoder.decodeList[BasicLabel]

  implicit val entityDecoder: EntityDecoder[IO, BasicLabels] =
    jsonOf[IO, BasicLabels]

  def encodeJson(project: BasicLabels): Json = {
    project.asJson
  }

  def decodeJson(n: String): BasicLabels = {
    decode[BasicLabels](n).right.get
  }

}
