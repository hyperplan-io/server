package com.foundaml.server.infrastructure.serialization

import com.foundaml.server.controllers.requests.PatchProjectRequest
import com.foundaml.server.domain.models._

import io.circe._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import cats.effect.IO
import cats.implicits._

object PatchProjectRequestSerializer {

  import com.foundaml.server.domain.models.ProblemType

  implicit val problemTypeEncoder = ProblemTypeSerializer.encoder

  implicit val algorithmPolicyDecoder =
    AlgorithmPolicySerializer.Implicits.decoder

  import com.foundaml.server.domain.models.AlgorithmPolicy
  implicit val decoder: Decoder[PatchProjectRequest] =
    (c: HCursor) =>
      for {
        name <- c.downField("name").as[Option[String]]
        policy <- c.downField("policy").as[Option[AlgorithmPolicy]]
      } yield PatchProjectRequest(name, policy)

  implicit val entityDecoder: EntityDecoder[IO, PatchProjectRequest] =
    jsonOf[IO, PatchProjectRequest]

}
