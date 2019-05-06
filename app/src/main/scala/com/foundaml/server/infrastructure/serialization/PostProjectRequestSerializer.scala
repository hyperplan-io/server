package com.foundaml.server.infrastructure.serialization

import com.foundaml.server.controllers.requests.PostProjectRequest

import io.circe._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import cats.effect.IO
import cats.implicits._

object PostProjectRequestSerializer {

  import com.foundaml.server.domain.models.ProblemType

  implicit val problemTypeEncoder = ProblemTypeSerializer.encoder
  implicit val problemTypeDecoder = ProblemTypeSerializer.decoder

  implicit val decoder: Decoder[PostProjectRequest] =
    (c: HCursor) =>
      for {
        id <- c.downField("id").as[String]
        name <- c.downField("name").as[String]
        problemType <- c.downField("type").as[ProblemType]
        featuresId <- c.downField("featuresId").as[String]
        labelsId <- c.downField("name").as[String]
      } yield PostProjectRequest(id, name, problemType, featuresId, labelsId)

  implicit val entityDecoder: EntityDecoder[IO, PostProjectRequest] =
    jsonOf[IO, PostProjectRequest]

}
