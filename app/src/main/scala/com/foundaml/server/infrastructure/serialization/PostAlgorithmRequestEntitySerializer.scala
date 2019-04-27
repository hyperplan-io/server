package com.foundaml.server.infrastructure.serialization

import com.foundaml.server.application.controllers.requests.PostAlgorithmRequest
import com.foundaml.server.domain.models.backends.Backend

import io.circe._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import cats.effect.IO
import cats.implicits._

object PostAlgorithmRequestEntitySerializer {

  implicit val backendDecoder: Decoder[Backend] = BackendSerializer.decoder

  implicit val decoder: Decoder[PostAlgorithmRequest] =
    (c: HCursor) =>
      for {
        id <- c.downField("id").as[String]
        projectId <- c.downField("projectId").as[String]
        backend <- c.downField("backend").as[Backend]
      } yield PostAlgorithmRequest(id, projectId, backend)

  implicit val entityDecoder: EntityDecoder[IO, PostAlgorithmRequest] =
    jsonOf[IO, PostAlgorithmRequest]

}
