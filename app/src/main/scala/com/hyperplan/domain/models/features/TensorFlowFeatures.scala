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

package com.hyperplan.domain.models.features

sealed trait TensorFlowFeature {
  val key: String
}
case class TensorFlowFloatFeature(key: String, value: Float)
    extends TensorFlowFeature
case class TensorFlowIntFeature(key: String, value: Int)
    extends TensorFlowFeature
case class TensorFlowStringFeature(key: String, value: String)
    extends TensorFlowFeature
case class TensorFlowFloatVectorFeature(key: String, value: List[Float])
    extends TensorFlowFeature
case class TensorFlowIntVectorFeature(key: String, value: List[Int])
    extends TensorFlowFeature
case class TensorFlowStringVectorFeature(key: String, value: List[String])
    extends TensorFlowFeature
case class TensorFlowEmptyVectorFeature(key: String) extends TensorFlowFeature
case class TensorFlowIntVector2dFeature(key: String, value: List[List[Int]])
    extends TensorFlowFeature
case class TensorFlowFloatVector2dFeature(key: String, value: List[List[Float]])
    extends TensorFlowFeature
case class TensorFlowStringVector2dFeature(
    key: String,
    value: List[List[String]]
) extends TensorFlowFeature

case class TensorFlowFeatures(
    signatureName: String,
    examples: List[TensorFlowFeature]
)
