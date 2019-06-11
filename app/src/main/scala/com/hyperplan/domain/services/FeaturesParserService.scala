package com.hyperplan.domain.services
import com.hyperplan.domain.models.features.Features._
import com.hyperplan.domain.models.ProjectConfiguration
import com.hyperplan.domain.models.ClassificationConfiguration
import com.hyperplan.domain.models.RegressionConfiguration
import com.hyperplan.domain.models.FeaturesConfiguration

import com.hyperplan.domain.models.features._

import cats.effect.IO
import cats.implicits._

import io.circe.Json
import io.circe.ACursor

object FeaturesParserService {
  def parseFeatures(configuration: ProjectConfiguration, hcursor: Json)(
      implicit domainService: DomainService
  ): IO[Features] = configuration match {
    case ClassificationConfiguration(featuresConfiguration, _) =>
      parseFeatures(
        featuresConfiguration,
        hcursor.hcursor.downField("features")
      )
    case RegressionConfiguration(featuresConfiguration) =>
      parseFeatures(
        featuresConfiguration,
        hcursor.hcursor.downField("features")
      )
  }

  def parseFeatures(
      configuration: FeaturesConfiguration,
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
          case (FloatFeatureType, One) =>
            IO.fromEither(jsonField.as[Float])
              .map[Features](value => List(FloatFeature(key, value)))
          case (FloatFeatureType, Vector) =>
            IO.fromEither(jsonField.as[List[Float]])
              .map[Features](value => List(FloatVectorFeature(key, value)))
          case (FloatFeatureType, Matrix) =>
            IO.fromEither(jsonField.as[List[List[Float]]])
              .map[Features](value => List(FloatVector2dFeature(key, value)))
          case (IntFeatureType, One) =>
            IO.fromEither(jsonField.as[Int])
              .map[Features](value => List(IntFeature(key, value)))
          case (IntFeatureType, Vector) =>
            IO.fromEither(jsonField.as[List[Int]])
              .map[Features](value => List(IntVectorFeature(key, value)))
          case (IntFeatureType, Matrix) =>
            IO.fromEither(jsonField.as[List[List[Int]]])
              .map[Features](value => List(IntVector2dFeature(key, value)))
          case (StringFeatureType, One) =>
            IO.fromEither(jsonField.as[String])
              .map[Features](value => List(StringFeature(key, value)))
          case (StringFeatureType, Vector) =>
            IO.fromEither(jsonField.as[List[String]])
              .map[Features](value => List(StringVectorFeature(key, value)))
          case (StringFeatureType, Matrix) =>
            IO.fromEither(jsonField.as[List[List[String]]])
              .map[Features](value => List(StringVector2dFeature(key, value)))
          case (ReferenceFeatureType(reference), One) =>
            domainService.readFeatures(reference).flatMap { config =>
              FeaturesParserService
                .parseFeatures(config, jsonField, prefix = s"${key}_")
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
