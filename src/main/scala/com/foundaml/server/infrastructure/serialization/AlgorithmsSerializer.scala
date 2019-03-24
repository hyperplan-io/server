package com.foundaml.server.infrastructure.serialization

import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.backends.Backend
import io.circe._
import io.circe.syntax._
import io.circe.parser.decode

object AlgorithmsSerializer {

  implicit val encoder: Encoder[Algorithm] =
    (algorithm: Algorithm) =>
      Json.obj(
        "id" -> Json.fromString(algorithm.id),
        "projectId" -> Json.fromString(algorithm.projectId),
        "backend" -> BackendSerializer.encodeJson(algorithm.backend)
      )

  implicit val decoder: Decoder[Algorithm] =
    (c: HCursor) =>
      for {
        id <- c.downField("id").as[String]
        projectId <- c.downField("projectId").as[String]
        backend <- c.downField("backend").as[Backend](BackendSerializer.decoder)
      } yield Algorithm(id, backend, projectId)

  def encodeJson(algorithm: Algorithm): Json = {
    algorithm.asJson
  }

  def decodeJson(n: String): Either[io.circe.Error, Algorithm] = {
    decode[Algorithm](n)
  }

}
