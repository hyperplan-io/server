package com.foundaml.server.infrastructure.serialization.labels

import com.foundaml.server.domain.models.labels.{ClassificationLabel, RegressionLabel}
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder, _}

object RegressionLabelSerializer {
  implicit val regressionlabelEncoder
  : Encoder[RegressionLabel] = (label: RegressionLabel) =>
    Json.obj(
      ("id", Json.fromString(label.id)),
      ("label", Json.fromFloatOrNull(label.label)),
      ("correctExampleUrl", Json.fromString(label.correctExampleUrl))
    )

  implicit val regressionLabelDecoder
  : Decoder[RegressionLabel] =
    (c: HCursor) =>
      for {
        id <- c.downField("id").as[String]
        label <- c.downField("label").as[Float]
        correctExampleUrl <- c.downField("correctExampleUrl").as[String]
      } yield RegressionLabel(id, label, correctExampleUrl)


  implicit val regressionLabelsSetEncoder: ArrayEncoder[Set[RegressionLabel]] = Encoder.encodeSet[RegressionLabel]
  implicit val regressionLabelsSetDecoder: Decoder[Set[RegressionLabel]] = Decoder.decodeSet[RegressionLabel]

  def encodeJsonNoSpaces(labels: RegressionLabel): String = {
    labels.asJson.noSpaces
  }

  def encodeJson(labels: RegressionLabel): Json = {
    labels.asJson
  }

  def encodeJsonSet(labels: Set[RegressionLabel]): Json = {
    labels.asJson
  }

  def encodeJsonSetNoSpaces(labels: Set[RegressionLabel]): String = {
    labels.asJson.noSpaces
  }

  def decodeJson(n: String): Either[io.circe.Error, RegressionLabel] = {
    decode[RegressionLabel](n)
  }

  def decodeJsonSet(n: String): Either[io.circe.Error, Set[RegressionLabel]] = {
    decode[Set[RegressionLabel]](n)
  }
}
