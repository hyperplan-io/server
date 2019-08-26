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

package com.hyperplan.domain.errors
import com.hyperplan.domain.models.features.FeatureDimension

sealed trait FeatureVectorDescriptorError extends Exception {
  val message: String
  override def getMessage = message
}

case class DuplicateFeatureIds() extends FeatureVectorDescriptorError {
  val message =
    "The feature names that you provided are not unique in the scope of the object"
}
case class FeatureVectorDescriptorDoesNotExistError(featuresId: String)
    extends FeatureVectorDescriptorError {
  val message = s"The features $featuresId does not exist"
}

case class FeatureVectorDescriptorAlreadyExistError(featuresId: String)
    extends FeatureVectorDescriptorError {
  val message = s"The feature $featuresId already exists"
}
case class ReferenceFeatureDoesNotExistError(reference: String)
    extends FeatureVectorDescriptorError {
  val message =
    s"The feature $reference does not exist and cannot be referenced"
}
case class UnsupportedDimensionError(
    message: String
) extends FeatureVectorDescriptorError
object UnsupportedDimensionError {
  def apply(
      featureClass: String,
      dimension: FeatureDimension
  ): UnsupportedDimensionError =
    UnsupportedDimensionError(
      s"The feature $featureClass cannot be used with dimension $dimension"
    )
}
case class RecursiveFeatureError(message: String)
    extends FeatureVectorDescriptorError
