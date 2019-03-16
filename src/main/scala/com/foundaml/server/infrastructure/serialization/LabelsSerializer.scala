package com.foundaml.server.infrastructure.serialization

import io.circe.generic.extras.Configuration
import io.circe._
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import com.foundaml.server.domain.models.labels._

object LabelsSerializer {

  import io.circe.generic.extras.semiauto._

  implicit val discriminator: Configuration =
    Configuration.default.withDiscriminator("class")

  implicit val labelEncoder: Encoder[Label] = LabelSerializer.encoder
  implicit val labelDecoder: Decoder[Label] = LabelSerializer.decoder

  implicit val encoder: Encoder[Labels] = deriveEncoder[Labels]
  implicit val decoder: Decoder[Labels] = deriveDecoder[Labels]



  def encodeJson(labels: Labels): String = {
    labels.asJson.noSpaces
  }

  def decodeJson(n: String): Labels = {
    decode[Labels](n).right.get
  }
}


object LabelSerializer {
  import io.circe.generic.extras.Configuration
  import io.circe.generic.extras.semiauto._

  implicit val discriminator: Configuration =
    Configuration.default.withDiscriminator("class")

  implicit val encoder: Encoder[Label] = deriveEncoder
  implicit val decoder: Decoder[Label] = deriveDecoder


  def encodeJson(labels: Label): String = {
    labels.asJson.noSpaces
  }

  def decodeJson(n: String): Label = {
    decode[Label](n).right.get
  }
}