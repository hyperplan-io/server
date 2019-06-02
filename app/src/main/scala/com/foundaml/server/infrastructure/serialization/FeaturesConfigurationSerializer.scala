package com.foundaml.server.infrastructure.serialization

import com.foundaml.server.domain.models.{
  FeatureConfiguration,
  FeaturesConfiguration
}
import com.foundaml.server.domain.models.features._
import io.circe
import io.circe.{HCursor, Json}
import io.circe.parser._
import io.circe.syntax._
import cats.effect.IO
import org.http4s.circe.jsonOf
import org.http4s.EntityDecoder

/**
  * Created by Antoine Sauray on 20/03/2019.
  */
object FeaturesConfigurationSerializer {

  import cats.syntax.functor._
  import io.circe.{Decoder, Encoder}
  import io.circe.syntax._

  implicit val featureDimensionEncoder: Encoder[FeatureDimension] = 
    (d: FeatureDimension) =>
      Json.fromString(d.name)

  implicit val featureDimensionDecoder: Decoder[FeatureDimension] = 
    (c: HCursor) =>
      c.as[String].flatMap {
        case One.name => Right(One)
        case Vector.name => Right(Vector)
        case Matrix.name => Right(Matrix)
      }

  implicit val customFeatureEncoder: Encoder[FeatureConfiguration] =
    (a: FeatureConfiguration) =>
      Json.obj(
        ("name", Json.fromString(a.name)),
        ("type", Json.fromString(a.featuresType)),
        ("dimension", a.dimension.asJson),
        ("description", Json.fromString(a.description))
      )

  implicit val customFeatureDecoder: Decoder[FeatureConfiguration] =
    (c: HCursor) =>
      for {
        name <- c.downField("name").as[String]
        featuresType <- c.downField("type").as[String]
        dimension <- c.downField("dimension").as[FeatureDimension]
        description <- c.downField("description").as[String]
      } yield {
        FeatureConfiguration(name, featuresType, dimension, description)
      }

  implicit val customFeatureListEncoder: Encoder[List[FeatureConfiguration]] =
    (a: List[FeatureConfiguration]) =>
      Json.fromValues(a.map(customFeatureEncoder.apply))

  implicit val customFeatureListDecoder: Decoder[List[FeatureConfiguration]] =
    (c: HCursor) =>
      Decoder.decodeList[FeatureConfiguration](customFeatureDecoder)(c)

  implicit val decoder: Decoder[FeaturesConfiguration] =
    (c: HCursor) =>
      for {
        id <- c.downField("id").as[String]
        data <- c
          .downField("data")
          .as[List[FeatureConfiguration]](
            customFeatureListDecoder
          )
      } yield FeaturesConfiguration(id, data)

  implicit val encoder: Encoder[FeaturesConfiguration] =
    (a: FeaturesConfiguration) =>
      Json.obj(
        ("id", Json.fromString(a.id)),
        ("data", Json.arr(a.data.map(customFeatureEncoder.apply): _*))
      )

  implicit val decoderList: Decoder[List[FeaturesConfiguration]] =
    (c: HCursor) => Decoder.decodeList[FeaturesConfiguration](decoder)(c)

  implicit val encoderList: Encoder[List[FeaturesConfiguration]] =
    (a: List[FeaturesConfiguration]) => Json.fromValues(a.map(encoder.apply))

  def encodeJson(featuresConfiguration: FeaturesConfiguration): Json = {
    featuresConfiguration.asJson
  }

  def encodeJsonList(
      featuresConfiguration: List[FeaturesConfiguration]
  ): Json = {
    featuresConfiguration.asJson
  }
  def encodeJsonNoSpaces(
      featuresConfiguration: FeaturesConfiguration
  ): String = {
    featuresConfiguration.asJson.noSpaces
  }

  def encodeJsonConfigurationNoSpaces(
      featuresConfiguration: FeatureConfiguration
  ): String = {
    featuresConfiguration.asJson.noSpaces
  }

  def encodeJsonConfigurationListNoSpaces(
      featuresConfiguration: List[FeatureConfiguration]
  ): String = {
    featuresConfiguration.asJson.noSpaces
  }

  def decodeJson(n: String): Either[io.circe.Error, FeaturesConfiguration] = {
    decode[FeaturesConfiguration](n)
  }

  def decodeFeatureConfigurationJson(
      n: String
  ): Either[io.circe.Error, FeatureConfiguration] = {
    decode[FeatureConfiguration](n)
  }

  def decodeFeatureConfigurationListJson(
      n: String
  ): Either[io.circe.Error, List[FeatureConfiguration]] = {
    decode[List[FeatureConfiguration]](n)
  }

  implicit val entityDecoder: EntityDecoder[IO, FeaturesConfiguration] =
    jsonOf[IO, FeaturesConfiguration]

}
