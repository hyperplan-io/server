package com.foundaml.server.services.infrastructure.serialization

import io.circe._

import com.foundaml.server.models._
import com.foundaml.server.models.features._

import cats.syntax.functor._
import io.circe.{Decoder, Encoder}, io.circe.generic.auto._
import io.circe.syntax._
import io.circe.syntax._
import io.circe.generic.extras.auto._
import io.circe.generic.extras.Configuration

import scalaz.zio.Task
import scalaz.zio.interop.catz._

import com.foundaml.server.models._

import io.circe._, io.circe.generic.semiauto._
import io.circe.parser.decode, io.circe.syntax._
import io.circe.syntax._

object ProblemTypeSerializer {

  def toString(problem: ProblemType)(implicit encoder: Encoder[ProblemType]): String = {
   problem.asJson.noSpaces 
  }

  def fromString(n: String)(implicit decoder: Decoder[ProblemType]): Either[io.circe.Error, ProblemType] = {
    decode[ProblemType](n)
  }
}
