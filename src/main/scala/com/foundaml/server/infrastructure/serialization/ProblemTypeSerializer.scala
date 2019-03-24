package com.foundaml.server.infrastructure.serialization

import io.circe.parser.decode
import io.circe.syntax._
import com.foundaml.server.domain.models._
import io.circe._

object ProblemTypeSerializer {

  implicit val encoder: Encoder[ProblemType] =
    (problemType: ProblemType) => Json.fromString(problemType.problemType)

  implicit val decoder: Decoder[ProblemType] =
    (c: HCursor) =>
      c.value.as[String].map {
        case Classification.problemType => Classification
        case Regression.problemType => Regression
      }

  def encodeJsonString(problem: ProblemType): String = {
    problem.asJson.noSpaces
  }

  def encodeJson(problem: ProblemType): Json = {
    problem.asJson
  }

  def decodeJson(n: String): Either[io.circe.Error, ProblemType] = {
    decode[ProblemType](n)
  }
}
