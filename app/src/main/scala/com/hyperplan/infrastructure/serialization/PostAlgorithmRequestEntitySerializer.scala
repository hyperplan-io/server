package com.hyperplan.infrastructure.serialization

import com.hyperplan.application.controllers.requests.PostAlgorithmRequest
import com.hyperplan.domain.models.backends.Backend
import com.hyperplan.domain.models.SecurityConfiguration

import io.circe._
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe.{jsonOf, jsonEncoderOf}

import cats.effect.IO
import cats.implicits._

object PostAlgorithmRequestEntitySerializer {

  implicit val backendDecoder: Decoder[Backend] = BackendSerializer.decoder
  implicit val backendEncoder: Encoder[Backend] = BackendSerializer.encoder
  implicit val securityConfigurationDecoder: Decoder[SecurityConfiguration] =
    (SecurityConfigurationSerializer.decoder)
  implicit val securityConfigurationEncoder: Encoder[SecurityConfiguration] =
    (SecurityConfigurationSerializer.encoder)

  implicit val decoder: Decoder[PostAlgorithmRequest] =
    (c: HCursor) =>
      for {
        id <- c.downField("id").as[String]
        projectId <- c.downField("projectId").as[String]
        backend <- c.downField("backend").as[Backend]
        security <- c.downField("security").as[SecurityConfiguration]
      } yield PostAlgorithmRequest(id, projectId, backend, security)

  implicit val encoder: Encoder[PostAlgorithmRequest] = Encoder.forProduct4(
    "id",
    "projectId",
    "backend",
    "security"
  )(r => (r.id, r.projectId, r.backend, r.security))
  implicit val entityDecoder: EntityDecoder[IO, PostAlgorithmRequest] =
    jsonOf[IO, PostAlgorithmRequest]

  implicit val entityEncoder: EntityEncoder[IO, PostAlgorithmRequest] =
    jsonEncoderOf[IO, PostAlgorithmRequest]

}
