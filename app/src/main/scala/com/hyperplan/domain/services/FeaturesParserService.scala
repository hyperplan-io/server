/*
 *
 * This file is part of the Hyperplan project.
 * Copyright (C) 2019  Hyperplan
 * Authors: Antoine Sauray
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gn/u.org/licenses/>.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the Hyperplan software without
 * disclosing the source code of your own applications.
 *
 *
 */

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
  def parseFeatures(
      domainService: DomainService
  )(configuration: ProjectConfiguration, hcursor: Json): IO[Features] =
    configuration match {
      case ClassificationConfiguration(featuresConfiguration, _, _) =>
        parseFeatures(
          domainService,
          featuresConfiguration,
          hcursor.hcursor.downField("features")
        )
      case RegressionConfiguration(featuresConfiguration, _) =>
        parseFeatures(
          domainService,
          featuresConfiguration,
          hcursor.hcursor.downField("features")
        )
    }

  def parseFeatures(
      domainService: DomainService,
      configuration: FeatureVectorDescriptor,
      hcursor: ACursor,
      prefix: String = ""
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
                  .parseFeatures(
                    domainService,
                    featuresConfig,
                    jsonField,
                    prefix = s"${key}_"
                  )
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
