package com.hyperplan.infrastructure.serialization

import com.foundaml.server.controllers.requests.PostProjectRequest

import io.circe._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import cats.effect.IO
import cats.implicits._

object PostProjectRequestSerializer {

  import com.hyperplan.domain.models.ProblemType

  implicit val problemTypeEncoder = ProblemTypeSerializer.encoder
  implicit val problemTypeDecoder = ProblemTypeSerializer.decoder

  implicit val decoder: Decoder[PostProjectRequest] =
    (c: HCursor) =>
      for {
        id <- c.downField("id").as[String]
        name <- c.downField("name").as[String]
        problemType <- c.downField("problem").as[ProblemType]
        featuresId <- c.downField("featuresId").as[String]
        labelsId <- c.downField("labelsId").as[Option[String]]
        topic <- c.downField("topic").as[Option[String]]
      } yield PostProjectRequest(id, name, problemType, featuresId, labelsId, topic)

  implicit val entityDecoder: EntityDecoder[IO, PostProjectRequest] =
    jsonOf[IO, PostProjectRequest]

}
