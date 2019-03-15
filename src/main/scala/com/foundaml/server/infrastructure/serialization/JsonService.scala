package com.foundaml.server.infrastructure.serialization

import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import scalaz.zio.{IO}
case class JsonError(message: String) extends Throwable {}

class JsonService {

  def toJson[Data](data: Data)(implicit encoder: io.circe.Encoder[Data]) =
    data.asJson.noSpaces
}
