package com.hyperplan.test

import cats.effect.IO
import io.circe.{Decoder, Encoder, HCursor, Json}
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

case class RasaNluEntityResult(name: String, confidence: Float)

object RasaNluEntityResult {
  implicit val encoder: Encoder[RasaNluEntityResult] =
    Encoder.forProduct2("name", "confidence")(
      rasa => (rasa.name, rasa.confidence)
    )

  implicit val decoder: Decoder[RasaNluEntityResult] = Decoder
    .forProduct2[RasaNluEntityResult, String, Float]("name", "confidence")(
      (name, confidence) => RasaNluEntityResult(name, confidence)
    )

}

case class RasaNluEntityResponse(
    intent: RasaNluEntityResult,
    intent_ranking: List[RasaNluEntityResult]
)

object RasaNluEntityResponse {
  implicit val encoder: Encoder[RasaNluEntityResponse] =
    Encoder.forProduct2("intent", "intent_ranking")(
      res => (res.intent, res.intent_ranking)
    )

  implicit val decoder: Decoder[RasaNluEntityResponse] =
    Decoder.forProduct2[RasaNluEntityResponse, RasaNluEntityResult, List[
      RasaNluEntityResult
    ]]("intent", "intent_ranking")(
      (intent, intentRanking) => RasaNluEntityResponse(intent, intentRanking)
    )

  implicit val decoderList: Decoder[List[RasaNluEntityResponse]] =
    Decoder.decodeList[RasaNluEntityResponse]
  implicit val entityEncoder: EntityEncoder[IO, RasaNluEntityResponse] =
    jsonEncoderOf[IO, RasaNluEntityResponse]

}
