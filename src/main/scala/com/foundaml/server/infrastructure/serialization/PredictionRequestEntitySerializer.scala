package com.foundaml.server.infrastructure.serialization

import io.circe.Decoder

import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import scalaz.zio.Task
import scalaz.zio.interop.catz._

import com.foundaml.server.application.controllers.requests.PredictionRequest

object PredictionRequestEntitySerializer {
  implicit val decoder: Decoder[PredictionRequest] =
    PredictionRequestSerializer.decoder
  val requestDecoder: EntityDecoder[Task, PredictionRequest] =
    jsonOf[Task, PredictionRequest]

}
