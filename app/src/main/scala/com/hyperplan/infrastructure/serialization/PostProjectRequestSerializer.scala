package com.hyperplan.infrastructure.serialization

import com.hyperplan.application.controllers.requests.PostProjectRequest
import io.circe._
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe.{jsonOf, jsonEncoderOf}
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
      } yield
        PostProjectRequest(id, name, problemType, featuresId, labelsId, topic)
  implicit val encoder: Encoder[PostProjectRequest] =
    Encoder.forProduct6(
      "id",
      "name",
      "problem",
      "featuresId",
      "labelsId",
      "topic"
    )(r => (r.id, r.name, r.problem, r.featuresId, r.labelsId, r.topic))

  implicit val entityDecoder: EntityDecoder[IO, PostProjectRequest] =
    jsonOf[IO, PostProjectRequest]

  implicit val entityEncoder: EntityEncoder[IO, PostProjectRequest] =
    jsonEncoderOf[IO, PostProjectRequest]

}
