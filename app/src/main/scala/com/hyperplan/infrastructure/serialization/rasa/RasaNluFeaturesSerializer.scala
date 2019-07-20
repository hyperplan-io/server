package com.hyperplan.infrastructure.serialization.rasa

import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

import io.circe.parser._
import io.circe.syntax._

import cats.effect.IO
import cats.implicits._

import com.hyperplan.domain.models.features.RasaNluFeatures

object RasaNluFeaturesSerializer {

  import io.circe._
  import io.circe.generic.semiauto._

  implicit val encoder: Encoder[RasaNluFeatures] =
    (features: RasaNluFeatures) =>
      Json.obj(
        ("q", Json.fromString(features.q)),
        ("project", Json.fromString(features.project)),
        ("model", Json.fromString(features.model))
      )
  implicit val decoder: Decoder[RasaNluFeatures] =
    (c: HCursor) =>
      for {
        q <- c.downField("q").as[String]
        project <- c.downField("project").as[String]
        model <- c.downField("model").as[String]
      } yield RasaNluFeatures(q, project, model)

  implicit val entityDecoder: EntityDecoder[IO, RasaNluFeatures] =
    jsonOf[IO, RasaNluFeatures]

  implicit val entityEncoder: EntityEncoder[IO, RasaNluFeatures] =
    jsonEncoderOf[IO, RasaNluFeatures]

  def encodeJson(features: RasaNluFeatures): Json = {
    features.asJson
  }

  def decodeJson(n: String): RasaNluFeatures = {
    decode[RasaNluFeatures](n).right.get
  }

}
