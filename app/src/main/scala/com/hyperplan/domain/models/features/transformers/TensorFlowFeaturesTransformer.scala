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

package com.hyperplan.domain.models.features.transformers

import com.hyperplan.domain.models.features.Features.Features
import com.hyperplan.domain.models.features._

case class TensorFlowFeaturesTransformer(
    signatureName: String,
    fields: Map[String, String]
) {

  def transform(
      features: Features,
      signatureName: String = signatureName,
      fields: Map[String, String] = fields
  ): TensorFlowFeatures = {

    val tensorFlowFeatures =
      features
        .flatMap { feature =>
          fields.get(feature.key).map { newKey =>
            feature -> newKey
          }
        }
        .foldLeft(TensorFlowFeatures(signatureName, List.empty)) {
          case (tfFeatures, feature) =>
            feature match {
              case (FloatFeature(key, value), field) =>
                tfFeatures.copy(
                  examples = TensorFlowFloatFeature(field, value) :: tfFeatures.examples
                )
              case (IntFeature(key, value), field) =>
                tfFeatures.copy(
                  examples = TensorFlowIntFeature(field, value) :: tfFeatures.examples
                )
              case (StringFeature(key, value), field) =>
                tfFeatures.copy(
                  examples = TensorFlowStringFeature(field, value) :: tfFeatures.examples
                )
              case (FloatArrayFeature(key, value), field) =>
                tfFeatures.copy(
                  examples = TensorFlowFloatVectorFeature(field, value) :: tfFeatures.examples
                )
              case (IntArrayFeature(key, value), field) =>
                tfFeatures.copy(
                  examples = TensorFlowIntVectorFeature(field, value) :: tfFeatures.examples
                )
              case (StringArrayFeature(key, value), field) =>
                tfFeatures.copy(
                  examples = TensorFlowStringVectorFeature(field, value) :: tfFeatures.examples
                )
              case (IntMatrixFeature(key, values), field) =>
                tfFeatures.copy(
                  examples = TensorFlowIntVector2dFeature(field, values) :: tfFeatures.examples
                )
              case (FloatMatrixFeature(key, values), field) =>
                tfFeatures.copy(
                  examples = TensorFlowFloatVector2dFeature(field, values) :: tfFeatures.examples
                )
              case (StringMatrixFeature(key, values), field) =>
                tfFeatures.copy(
                  examples = TensorFlowStringVector2dFeature(field, values) :: tfFeatures.examples
                )
              case (ReferenceFeature(key, reference, values), field) =>
                val newFields = values.map { feature =>
                  feature.key -> s"$field\_$feature"
                }.toMap
                tfFeatures.copy(
                  examples = transform(values, signatureName, newFields).examples ::: tfFeatures.examples
                )
            }

        }
    tensorFlowFeatures
  }

}
