package com.hyperplan.infrastructure.serialization

import cats.effect.IO
import cats.implicits._

import io.circe._
import io.circe.syntax._
import io.circe.parser.decode

import org.http4s.circe.{jsonOf, jsonEncoderOf}

import com.hyperplan.domain.models._
import com.hyperplan.domain.models.backends.Backend
import org.http4s.{EntityDecoder, EntityEncoder}

object AlgorithmsSerializer {

  implicit val encoder: Encoder[Algorithm] =
    (algorithm: Algorithm) =>
      Json.obj(
        "id" -> Json.fromString(algorithm.id),
        "projectId" -> Json.fromString(algorithm.projectId),
        "backend" -> BackendSerializer.encodeJson(algorithm.backend),
        "security" -> SecurityConfigurationSerializer.encodeJson(
          algorithm.security
        )
      )

  implicit val decoder: Decoder[Algorithm] =
    (c: HCursor) =>
      for {
        id <- c.downField("id").as[String]
        projectId <- c.downField("projectId").as[String]
        backend <- c.downField("backend").as[Backend](BackendSerializer.decoder)
        security <- c
          .downField("security")
          .as[SecurityConfiguration](SecurityConfigurationSerializer.decoder)
      } yield Algorithm(id, backend, projectId, security)

  def encodeJson(algorithm: Algorithm): Json = {
    algorithm.asJson
  }

  def decodeJson(n: String): Either[io.circe.Error, Algorithm] = {
    decode[Algorithm](n)
  }

  implicit val entityDecoder: EntityDecoder[IO, Algorithm] =
    jsonOf[IO, Algorithm]

  implicit val entityEncoder: EntityEncoder[IO, Algorithm] =
    jsonEncoderOf[IO, Algorithm]
}
