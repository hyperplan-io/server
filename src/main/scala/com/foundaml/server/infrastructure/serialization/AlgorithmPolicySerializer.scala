package com.foundaml.server.infrastructure.serialization

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.extras.Configuration
import io.circe.parser.decode
import io.circe.syntax._
import com.foundaml.server.domain.models._

object AlgorithmPolicySerializer {

  object Implicits {
    implicit val discriminator: Configuration =
      Configuration.default.withDiscriminator("class")

    implicit val encoder: Encoder[AlgorithmPolicy] = {
      case policy: NoAlgorithm =>
        Json.obj("class" -> Json.fromString(policy.name))
      case policy: DefaultAlgorithm =>
        Json.obj(
          "class" -> Json.fromString(policy.name),
          "algorithmId" -> Json.fromString(policy.algorithmId)
        )

    }
    implicit val decoder: Decoder[AlgorithmPolicy] =
      (c: HCursor) =>
        c.downField("class").as[String].flatMap {
          case NoAlgorithm.name => Right(NoAlgorithm())
          case DefaultAlgorithm.name =>
            c.downField("algorithmId").as[String].map { algorithmId =>
              DefaultAlgorithm(algorithmId)
            }
        }
  }

  import Implicits._

  def encodeJsonString(policy: AlgorithmPolicy): String = {
    policy.asJson.noSpaces
  }

  def encodeJson(policy: AlgorithmPolicy): Json = {
    policy.asJson
  }

  def decodeJson(n: String): Either[io.circe.Error, AlgorithmPolicy] = {
    decode[AlgorithmPolicy](n)
  }
}
