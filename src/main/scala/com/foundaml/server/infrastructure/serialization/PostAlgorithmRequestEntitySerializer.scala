package com.foundaml.server.infrastructure.serialization

import com.foundaml.server.application.controllers.requests.PostAlgorithmRequest
import com.foundaml.server.domain.models.backends.Backend

import io.circe._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import scalaz.zio.Task
import scalaz.zio.interop.catz._

object PostAlgorithmRequestEntitySerializer {


  implicit val backendDecoder: Decoder[Backend] =
    BackendSerializer.Implicits.backendDecoder


  implicit val decoder: Decoder[PostAlgorithmRequest] =
    (c: HCursor) =>
      for {
        id <- c.downField("id").as[String]
        projectId <- c.downField("projectId").as[String]
        backend <- c.downField("problem").as[Backend]
      } yield PostAlgorithmRequest(id, projectId, backend)


  implicit val entityDecoder: EntityDecoder[Task, PostAlgorithmRequest] =
    jsonOf[Task, PostAlgorithmRequest]

}
