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

object Features {
  type Features = List[Feature]
}

sealed trait Feature {
  val dimension: FeatureDimension
  val featureType: FeatureType
  val key: String
}

sealed trait FeatureType {
  val name: String
}

case object FloatFeatureType extends FeatureType {
  val name = "float"
}

case object IntFeatureType extends FeatureType {
  val name = "int"
}

case object StringFeatureType extends FeatureType {
  val name = "string"
}

case class ReferenceFeatureType(name: String) extends FeatureType

sealed trait FeatureDimension {
  val name: String
}
case object Scalar extends FeatureDimension {
  val name = "scalar"
}
case object Array extends FeatureDimension {
  val name = "array"
}
case object Matrix extends FeatureDimension {
  val name = "matrix"
}

case class FloatFeature(key: String, value: Float) extends Feature {
  val dimension = Scalar
  val featureType = FloatFeatureType
}

case class FloatArrayFeature(key: String, data: List[Float]) extends Feature {
  val dimension = Array
  val featureType = FloatFeatureType
}

case class FloatMatrixFeature(key: String, data: List[List[Float]])
    extends Feature {
  val dimension = Matrix
  val featureType = FloatFeatureType
}

case class IntFeature(key: String, value: Int) extends Feature {
  val dimension = Scalar
  val featureType = IntFeatureType
}

case class IntArrayFeature(key: String, data: List[Int]) extends Feature {
  val dimension = Array
  val featureType = IntFeatureType
}

case class IntMatrixFeature(key: String, data: List[List[Int]])
    extends Feature {
  val dimension = Matrix
  val featureType = IntFeatureType
}

case class StringFeature(key: String, value: String) extends Feature {
  val dimension = Scalar
  val featureType = StringFeatureType
}

case class StringArrayFeature(key: String, data: List[String]) extends Feature {
  val dimension = Array
  val featureType = StringFeatureType
}

case class StringMatrixFeature(key: String, data: List[List[String]])
    extends Feature {
  val dimension = Matrix
  val featureType = StringFeatureType
}

case class ReferenceFeature(
    key: String,
    reference: String,
    value: Features.Features
) extends Feature {
  val dimension = Scalar
  val featureType = ReferenceFeatureType(reference)
}
