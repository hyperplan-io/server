package com.hyperplan.infrastructure.serialization

import com.hyperplan.domain.models.{FeatureDescriptor, FeatureVectorDescriptor}
import com.hyperplan.domain.models.features._
import io.circe
import io.circe.{HCursor, Json}
import io.circe.parser._
import io.circe.syntax._
import cats.effect.IO
import org.http4s.circe.{jsonOf, jsonEncoderOf}
import org.http4s.EntityDecoder
import org.http4s.EntityEncoder

/**
  * Created by Antoine Sauray on 20/03/2019.
  */
object FeaturesConfigurationSerializer {

  import cats.syntax.functor._
  import io.circe.{Decoder, Encoder}
  import io.circe.syntax._

  implicit val featureDimensionEncoder: Encoder[FeatureDimension] =
    (d: FeatureDimension) => Json.fromString(d.name)

  implicit val featureDimensionDecoder: Decoder[FeatureDimension] =
    (c: HCursor) =>
      c.as[String].flatMap {
        case Scalar.name => Right(Scalar)
        case Array.name => Right(Array)
        case Matrix.name => Right(Matrix)
      }

  implicit val featureTypeEncoder: Encoder[FeatureType] =
    (d: FeatureType) => Json.fromString(d.name)

  implicit val featureTypeDecoder: Decoder[FeatureType] =
    (c: HCursor) =>
      c.as[String].flatMap {
        case FloatFeatureType.name => Right(FloatFeatureType)
        case IntFeatureType.name => Right(IntFeatureType)
        case StringFeatureType.name => Right(StringFeatureType)
        case reference => Right(ReferenceFeatureType(reference))
      }

  implicit val customFeatureEncoder: Encoder[FeatureDescriptor] =
    (a: FeatureDescriptor) =>
      Json.obj(
        ("name", Json.fromString(a.name)),
        ("type", a.featuresType.asJson),
        ("dimension", a.dimension.asJson),
        ("description", Json.fromString(a.description))
      )

  implicit val customFeatureDecoder: Decoder[FeatureDescriptor] =
    (c: HCursor) =>
      for {
        name <- c.downField("name").as[String]
        featuresType <- c.downField("type").as[FeatureType]
        dimension <- c.downField("dimension").as[FeatureDimension]
        description <- c.downField("description").as[String]
      } yield {
        FeatureDescriptor(name, featuresType, dimension, description)
      }

  implicit val customFeatureListEncoder: Encoder[List[FeatureDescriptor]] =
    (a: List[FeatureDescriptor]) =>
      Json.fromValues(a.map(customFeatureEncoder.apply))

  implicit val customFeatureListDecoder: Decoder[List[FeatureDescriptor]] =
    (c: HCursor) =>
      Decoder.decodeList[FeatureDescriptor](customFeatureDecoder)(c)

  implicit val decoder: Decoder[FeatureVectorDescriptor] =
    (c: HCursor) =>
      for {
        id <- c.downField("id").as[String]
        data <- c
          .downField("data")
          .as[List[FeatureDescriptor]](
            customFeatureListDecoder
          )
      } yield FeatureVectorDescriptor(id, data)

  implicit val encoder: Encoder[FeatureVectorDescriptor] =
    (a: FeatureVectorDescriptor) =>
      Json.obj(
        ("id", Json.fromString(a.id)),
        ("data", Json.arr(a.data.map(customFeatureEncoder.apply): _*))
      )

  implicit val decoderList: Decoder[List[FeatureVectorDescriptor]] =
    (c: HCursor) => Decoder.decodeList[FeatureVectorDescriptor](decoder)(c)

  implicit val encoderList: Encoder[List[FeatureVectorDescriptor]] =
    (a: List[FeatureVectorDescriptor]) => Json.fromValues(a.map(encoder.apply))

  def encodeJson(featuresConfiguration: FeatureVectorDescriptor): Json = {
    featuresConfiguration.asJson
  }

  def encodeJsonList(
      featuresConfiguration: List[FeatureVectorDescriptor]
  ): Json = {
    featuresConfiguration.asJson
  }
  def encodeJsonNoSpaces(
      featuresConfiguration: FeatureVectorDescriptor
  ): String = {
    featuresConfiguration.asJson.noSpaces
  }

  def encodeJsonConfigurationNoSpaces(
      featuresConfiguration: FeatureDescriptor
  ): String = {
    featuresConfiguration.asJson.noSpaces
  }

  def encodeJsonConfigurationListNoSpaces(
      featuresConfiguration: List[FeatureDescriptor]
  ): String = {
    featuresConfiguration.asJson.noSpaces
  }

  def decodeJson(n: String): Either[io.circe.Error, FeatureVectorDescriptor] = {
    decode[FeatureVectorDescriptor](n)
  }

  def decodeFeatureConfigurationJson(
      n: String
  ): Either[io.circe.Error, FeatureDescriptor] = {
    decode[FeatureDescriptor](n)
  }

  def decodeFeatureConfigurationListJson(
      n: String
  ): Either[io.circe.Error, List[FeatureDescriptor]] = {
    decode[List[FeatureDescriptor]](n)
  }

  implicit val entityDecoder: EntityDecoder[IO, FeatureVectorDescriptor] =
    jsonOf[IO, FeatureVectorDescriptor]

  implicit val entityEncoder: EntityEncoder[IO, FeatureVectorDescriptor] =
    jsonEncoderOf[IO, FeatureVectorDescriptor]

  implicit val entityDecoderList
      : EntityDecoder[IO, List[FeatureVectorDescriptor]] =
    jsonOf[IO, List[FeatureVectorDescriptor]]
}
