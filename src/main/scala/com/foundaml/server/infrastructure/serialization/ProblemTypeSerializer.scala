package com.foundaml.server.infrastructure.serialization

import io.circe.generic.extras.Configuration, io.circe.generic.extras.auto._
import io.circe.parser.decode
import io.circe.syntax._

import com.foundaml.server.domain.models._

object ProblemTypeSerializer {

  implicit val discriminator: Configuration = CirceEncoders.discriminator

  def encodeJson(problem: ProblemType): String = {
    problem.asJson.noSpaces
  }

  def decodeJson(n: String): Either[io.circe.Error, ProblemType] = {
    decode[ProblemType](n)
  }
}
