package com.foundaml.server.infrastructure.serialization.labels

import com.foundaml.server.domain.models.labels.ClassificationLabel
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import org.http4s.circe.jsonEncoderOf
import io.circe._

object ClassificationLabelSerializer {
  implicit val classificationlabelEncoder: Encoder[ClassificationLabel] =
    (label: ClassificationLabel) =>
      Json.obj(
        ("id", Json.fromString(label.id)),
        ("label", Json.fromString(label.label)),
        ("probability", Json.fromFloatOrNull(label.probability)),
        ("correctExampleUrl", Json.fromString(label.correctExampleUrl)),
        ("incorrectExampleUrl", Json.fromString(label.incorrectExampleUrl))
      )

  implicit val classificationLabelDecoder: Decoder[ClassificationLabel] =
    (c: HCursor) =>
      for {
        id <- c.downField("id").as[String]
        label <- c.downField("label").as[String]
        probability <- c.downField("probability").as[Float]
        correctExampleUrl <- c.downField("correctExampleUrl").as[String]
        incorrectExampleUrl <- c.downField("incorrectExampleUrl").as[String]
      } yield
        ClassificationLabel(
          id,
          label,
          probability,
          correctExampleUrl,
          incorrectExampleUrl
        )

  implicit val classificationLabelsSetEncoder
      : ArrayEncoder[Set[ClassificationLabel]] =
    Encoder.encodeSet[ClassificationLabel]
  implicit val classificationLabelsSetDecoder
      : Decoder[Set[ClassificationLabel]] =
    Decoder.decodeSet[ClassificationLabel]

  def encodeJsonNoSpaces(labels: ClassificationLabel): String = {
    labels.asJson.noSpaces
  }

  def encodeJson(labels: ClassificationLabel): Json = {
    labels.asJson
  }

  def encodeJsonSet(labels: Set[ClassificationLabel]): Json = {
    labels.asJson
  }

  def encodeJsonSetNoSpaces(labels: Set[ClassificationLabel]): String = {
    labels.asJson.noSpaces
  }

  def decodeJson(n: String): Either[io.circe.Error, ClassificationLabel] = {
    decode[ClassificationLabel](n)
  }

  def decodeJsonSet(
      n: String
  ): Either[io.circe.Error, Set[ClassificationLabel]] = {
    decode[Set[ClassificationLabel]](n)
  }
}
