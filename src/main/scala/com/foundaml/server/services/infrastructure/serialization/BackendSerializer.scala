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

import com.foundaml.server.models.backends._

import io.circe._, io.circe.generic.semiauto._
import io.circe.parser.decode, io.circe.syntax._
import io.circe.parser.decode
import io.circe.syntax._

object BackendSerializer {

  def toString(backend: Backend)(implicit encoder: Encoder[Backend]): String = {
   backend.asJson.noSpaces 
  }

  def fromString(n: String)(implicit decoder: Decoder[Backend]): Backend = {
    decode[Backend](n).right.get
  }
}
