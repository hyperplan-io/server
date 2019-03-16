package com.foundaml.server.infrastructure.serialization

import io.circe.parser.decode
import io.circe.syntax._
import com.foundaml.server.domain.models.{Algorithm, AlgorithmPolicy, ProblemType, Project}

object ProjectSerializer {

  import io.circe._, io.circe.generic.semiauto._

  implicit val algorithmPolicyEncoder: Encoder[AlgorithmPolicy] = AlgorithmPolicySerializer.encoder
  implicit val algorithmPolicyDecoder: Decoder[AlgorithmPolicy] = AlgorithmPolicySerializer.decoder

  implicit val problemTypeEncoder: Encoder[ProblemType] = ProblemTypeSerializer.encoder
  implicit val problemTypeDecoder: Decoder[ProblemType] = ProblemTypeSerializer.decoder

  implicit val algorithmEncoder: Encoder[Algorithm] = AlgorithmsSerializer.encoder
  implicit val algorithmDecoder: Decoder[Algorithm] = AlgorithmsSerializer.decoder


  implicit val encoder: Encoder[Project] = deriveEncoder
  implicit val decoder: Decoder[Project] = deriveDecoder


  def encodeJson(project: Project): String = {
    project.asJson.noSpaces
  }

  def decodeJson(n: String): Project = {
    decode[Project](n).right.get
  }
}
