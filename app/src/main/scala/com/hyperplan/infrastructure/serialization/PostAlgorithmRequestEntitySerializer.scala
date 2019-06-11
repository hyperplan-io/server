package com.hyperplan.infrastructure.serialization

import com.hyperplan.application.controllers.requests.PostAlgorithmRequest
import com.hyperplan.domain.models.backends.Backend
import com.hyperplan.domain.models.SecurityConfiguration

import io.circe._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import cats.effect.IO
import cats.implicits._

object PostAlgorithmRequestEntitySerializer {

  implicit val backendDecoder: Decoder[Backend] = BackendSerializer.decoder
  implicit val securityConfigurationDecoder: Decoder[SecurityConfiguration] =
    (SecurityConfigurationSerializer.decoder)

  implicit val decoder: Decoder[PostAlgorithmRequest] =
    (c: HCursor) =>
      for {
        id <- c.downField("id").as[String]
        projectId <- c.downField("projectId").as[String]
        backend <- c.downField("backend").as[Backend]
        security <- c.downField("security").as[SecurityConfiguration]
      } yield PostAlgorithmRequest(id, projectId, backend, security)

  implicit val entityDecoder: EntityDecoder[IO, PostAlgorithmRequest] =
    jsonOf[IO, PostAlgorithmRequest]

}
