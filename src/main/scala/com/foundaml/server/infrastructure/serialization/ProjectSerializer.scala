package com.foundaml.server.infrastructure.serialization

import io.circe.parser.decode
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

import com.foundaml.server.domain.models.Project

object ProjectSerializer {

  implicit val encoder: Encoder[Project] = implicitly[Encoder[Project]]
  implicit val decoder: Decoder[Project] = implicitly[Decoder[Project]]

  def encodeJson(project: Project): String = {
    project.asJson.noSpaces
  }

  def decodeJson(n: String): Project = {
    decode[Project](n).right.get
  }
}
