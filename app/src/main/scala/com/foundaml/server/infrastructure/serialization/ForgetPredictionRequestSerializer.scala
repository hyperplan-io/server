package com.foundaml.server.infrastructure.serialization

import com.foundaml.server.controllers.requests.ForgetPredictionRequest

import io.circe._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import cats.effect.IO
import cats.implicits._

object ForgetPredictionRequestSerializer {

  implicit val decoder: Decoder[ForgetPredictionRequest] =
    (c: HCursor) =>
      for {
        entityName <- c.downField("entityName").as[String]
        entityId <- c.downField("entityId").as[String]
      } yield ForgetPredictionRequest(entityName, entityId)

  implicit val entityDecoder: EntityDecoder[IO, ForgetPredictionRequest] =
    jsonOf[IO, ForgetPredictionRequest]

}
