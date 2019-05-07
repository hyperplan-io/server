package com.foundaml.server.infrastructure.serialization

import io.circe._
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import com.foundaml.server.domain.models.backends.{
  Backend,
  LocalClassification,
  TensorFlowClassificationBackend,
  TensorFlowRegressionBackend
}
import com.foundaml.server.domain.models.FeaturesConfiguration
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import cats.effect.IO

object DomainClassSerializer {
/*
  implicit val encoder: Encoder[DomainClass] =
    (domainClass: DomainClass) =>
      Json.obj(
        (
          "id",
          Json.fromString(domainClass.id)
        ),
        ("data", FeaturesConfigurationSerializer.encodeJson(domainClass.data))
      )

  implicit val decoder: Decoder[DomainClass] =
    (c: HCursor) =>
      for {
        id <- c.downField("id").as[String]
        data <- c
          .downField("data")
          .as[FeaturesConfiguration](FeaturesConfigurationSerializer.decoder)
      } yield
        DomainClass(
          id,
          data
        )

  implicit val entityDecoder: EntityDecoder[IO, DomainClass] =
    jsonOf[IO, DomainClass]

  def encodeJsonNoSpaces(domainClass: DomainClass): String = {
    domainClass.asJson.noSpaces
  }

  def encodeJson(domainClass: DomainClass): Json = {
    domainClass.asJson
  }

  def decodeJson(n: String): Either[io.circe.Error, DomainClass] = {
    decode[DomainClass](n)
  }
  */
}
