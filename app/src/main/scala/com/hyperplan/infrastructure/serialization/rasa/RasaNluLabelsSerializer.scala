package com.hyperplan.infrastructure.serialization.rasa

import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import io.circe.parser._
import io.circe.syntax._

import cats.effect.IO
import cats.implicits._
import com.hyperplan.domain.models.labels.RasaNluClassificationLabels
import com.hyperplan.domain.models.labels.RasaNluIntent

object RasaNluLabelsSerializer {

  import io.circe._
  import io.circe.generic.semiauto._

  implicit val predictionEncoder: Encoder[RasaNluIntent] = deriveEncoder
  implicit val predictionDecoder: Decoder[RasaNluIntent] = deriveDecoder

  implicit val encoder: Encoder[RasaNluClassificationLabels] = deriveEncoder
  implicit val decoder: Decoder[RasaNluClassificationLabels] = deriveDecoder

  implicit val entityDecoder: EntityDecoder[IO, RasaNluClassificationLabels] =
    jsonOf[IO, RasaNluClassificationLabels]

  def encodeJson(project: RasaNluClassificationLabels): Json = {
    project.asJson
  }

  def decodeJson(n: String): RasaNluClassificationLabels = {
    decode[RasaNluClassificationLabels](n).right.get
  }

}
