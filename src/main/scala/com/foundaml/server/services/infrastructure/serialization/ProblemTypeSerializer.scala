package com.foundaml.server.services.infrastructure.serialization

import io.circe._

import io.circe.generic.extras.Configuration, io.circe.generic.extras.auto._

import scalaz.zio.Task
import scalaz.zio.interop.catz._

import com.foundaml.server.models._

import io.circe.parser.decode
import io.circe.syntax._

object ProblemTypeSerializer {

  implicit val discriminator: Configuration = CirceEncoders.discriminator


  def encodeJson(problem: ProblemType): String = {
   problem.asJson.noSpaces 
  }

  def decodeJson(n: String): Either[io.circe.Error, ProblemType] = {
    decode[ProblemType](n)
  }
}
