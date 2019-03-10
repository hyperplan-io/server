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
import io.circe.parser.decode
import io.circe.syntax._

object ProblemTypeSerializer {

  val discriminator: Configuration = Configuration.default.withDiscriminator("problem")

  def toString(labels: ProblemType)(implicit encoder: Encoder[ProblemType]): String = {
   labels.asJson.noSpaces 
  }

  def fromString(n: String)(implicit decoder: Decoder[ProblemType]): ProblemType = {
    decode[ProblemType](n).right.get
  }
}
