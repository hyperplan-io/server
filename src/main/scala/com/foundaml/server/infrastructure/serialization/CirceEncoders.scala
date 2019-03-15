package com.foundaml.server.infrastructure.serialization

import io.circe._
import io.circe.generic.semiauto._

import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.features._

import com.foundaml.server.application.controllers.requests._

import cats.syntax.functor._
import io.circe.{Decoder, Encoder}, io.circe.generic.auto._
import io.circe.syntax._
import io.circe.generic.extras.auto._
import io.circe.generic.extras.Configuration

import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._

import scalaz.zio.Task
import scalaz.zio.interop.catz._

import io.circe._, io.circe.generic.semiauto._
import io.circe.parser.decode, io.circe.syntax._
import io.circe.syntax._

import ProblemTypeSerializer._

object CirceEncoders {

  implicit val discriminator: Configuration =
    Configuration.default.withDiscriminator("class")

  implicit val predictionDecoder: Decoder[Prediction] =
    deriveDecoder[Prediction]
  implicit val predictionEncoder: Encoder[Prediction] =
    deriveEncoder[Prediction]
}
