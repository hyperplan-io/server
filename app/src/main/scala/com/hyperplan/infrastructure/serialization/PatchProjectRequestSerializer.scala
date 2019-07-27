package com.hyperplan.infrastructure.serialization

import com.hyperplan.application.controllers.requests.PatchProjectRequest
import com.hyperplan.domain.models._
import io.circe._
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import cats.effect.IO
import cats.implicits._

object PatchProjectRequestSerializer {

  import com.hyperplan.domain.models.ProblemType

  implicit val problemTypeEncoder = ProblemTypeSerializer.encoder

  implicit val algorithmPolicyDecoder =
    AlgorithmPolicySerializer.Implicits.decoder
  implicit val algorithmPolicyEncoder =
    AlgorithmPolicySerializer.Implicits.encoder

  import com.hyperplan.domain.models.AlgorithmPolicy
  implicit val decoder: Decoder[PatchProjectRequest] =
    (c: HCursor) =>
      for {
        name <- c.downField("name").as[Option[String]]
        policy <- c.downField("policy").as[Option[AlgorithmPolicy]]
      } yield PatchProjectRequest(name, policy)

  implicit val encoder: Encoder[PatchProjectRequest] =
    Encoder.forProduct2("name", "policy")(r => (r.name, r.policy))

  implicit val entityDecoder: EntityDecoder[IO, PatchProjectRequest] =
    jsonOf[IO, PatchProjectRequest]

  implicit val entityEncoder: EntityEncoder[IO, PatchProjectRequest] =
    jsonEncoderOf[IO, PatchProjectRequest]

}
