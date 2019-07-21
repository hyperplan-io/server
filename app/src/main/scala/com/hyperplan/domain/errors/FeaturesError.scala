package com.hyperplan.domain.errors
import com.hyperplan.domain.models.features.FeatureDimension

sealed trait FeaturesError

case class FeaturesDoesNotExistError(featuresId: String) extends FeaturesError
case class FeaturesAlreadyExistError(featuresId: String) extends FeaturesError
case class ReferenceFeatureDoesNotExistError() extends FeaturesError
case class UnsupportedDimensionError(
    featureClass: String,
    dimension: FeatureDimension
) extends FeaturesError
case class RecursiveFeatureError(featureClass: String, dimension: String)
    extends FeaturesError
