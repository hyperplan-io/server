package com.foundaml.server.infrastructure.serialization

import io.circe.parser.decode
import io.circe.syntax._
import com.foundaml.server.domain.models._

object ProjectSerializer {

  import io.circe._, io.circe.generic.semiauto._

  implicit val algorithmPolicyEncoder: Encoder[AlgorithmPolicy] =
    AlgorithmPolicySerializer.Implicits.encoder
  implicit val algorithmPolicyDecoder: Decoder[AlgorithmPolicy] =
    AlgorithmPolicySerializer.Implicits.decoder

  implicit val problemTypeEncoder: Encoder[ProblemType] =
    ProblemTypeSerializer.encoder
  implicit val problemTypeDecoder: Decoder[ProblemType] =
    ProblemTypeSerializer.decoder

  implicit val algorithmEncoder: Encoder[Algorithm] =
    AlgorithmsSerializer.Implicits.encoder
  implicit val algorithmDecoder: Decoder[Algorithm] =
    AlgorithmsSerializer.Implicits.decoder

  implicit val projectConfigurationEncoder: Encoder[ProjectConfiguration] =
    deriveEncoder
  implicit val projectConfigurationDecoder: Decoder[ProjectConfiguration] =
    deriveDecoder

  implicit val encoder: Encoder[Project] = deriveEncoder
  implicit val decoder: Decoder[Project] = deriveDecoder

  def encodeJson(project: Project): Json = {
    project.asJson
  }

  def decodeJson(n: String): Project = {
    decode[Project](n).right.get
  }
}
