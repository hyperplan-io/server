package com.hyperplan.domain.services
import com.hyperplan.domain.models.features.Features._
import com.hyperplan.domain.models.ProjectConfiguration
import com.hyperplan.domain.models.ClassificationConfiguration
import com.hyperplan.domain.models.RegressionConfiguration
import com.hyperplan.domain.models.FeatureVectorDescriptor

import com.hyperplan.domain.models.features._

import cats.effect.IO
import cats.implicits._

import io.circe.Json
import io.circe.ACursor

object FeaturesParserService {
  def parseFeatures(configuration: ProjectConfiguration, hcursor: Json)(
      implicit domainService: DomainService
  ): IO[Features] = configuration match {
    case ClassificationConfiguration(featuresConfiguration, _, _) =>
      parseFeatures(
        featuresConfiguration,
        hcursor.hcursor.downField("features")
      )
    case RegressionConfiguration(featuresConfiguration, _) =>
      parseFeatures(
        featuresConfiguration,
        hcursor.hcursor.downField("features")
      )
  }

  def parseFeatures(
      configuration: FeatureVectorDescriptor,
      hcursor: ACursor,
      prefix: String = ""
  )(
      implicit domainService: DomainService
  ): IO[Features] = {
    configuration.data
      .map { featuresConfiguration =>
        val key = s"${prefix}${featuresConfiguration.name}"
        val jsonField = hcursor.downField(featuresConfiguration.name)
        (featuresConfiguration.featuresType, featuresConfiguration.dimension) match {
          case (FloatFeatureType, Scalar) =>
            IO.fromEither(jsonField.as[Float])
              .map[Features](value => List(FloatFeature(key, value)))
          case (FloatFeatureType, Array) =>
            IO.fromEither(jsonField.as[List[Float]])
              .map[Features](value => List(FloatArrayFeature(key, value)))
          case (FloatFeatureType, Matrix) =>
            IO.fromEither(jsonField.as[List[List[Float]]])
              .map[Features](value => List(FloatMatrixFeature(key, value)))
          case (IntFeatureType, Scalar) =>
            IO.fromEither(jsonField.as[Int])
              .map[Features](value => List(IntFeature(key, value)))
          case (IntFeatureType, Array) =>
            IO.fromEither(jsonField.as[List[Int]])
              .map[Features](value => List(IntArrayFeature(key, value)))
          case (IntFeatureType, Matrix) =>
            IO.fromEither(jsonField.as[List[List[Int]]])
              .map[Features](value => List(IntMatrixFeature(key, value)))
          case (StringFeatureType, Scalar) =>
            IO.fromEither(jsonField.as[String])
              .map[Features](value => List(StringFeature(key, value)))
          case (StringFeatureType, Array) =>
            IO.fromEither(jsonField.as[List[String]])
              .map[Features](value => List(StringArrayFeature(key, value)))
          case (StringFeatureType, Matrix) =>
            IO.fromEither(jsonField.as[List[List[String]]])
              .map[Features](value => List(StringMatrixFeature(key, value)))
          case (ReferenceFeatureType(reference), Scalar) =>
            domainService.readFeatures(reference).flatMap {
              case Some(featuresConfig) =>
                FeaturesParserService
                  .parseFeatures(featuresConfig, jsonField, prefix = s"${key}_")
              case None =>
                ???
            }
          case (reference, dimension) =>
            IO.raiseError(
              new Exception(
                s"The reference $reference features cannot be of dimension $dimension"
              )
            )
        }
      }
      .sequence
      .map(_.flatten)
  }
}
