package com.hyperplan.domain.errors

sealed trait FeaturesError

case class FeaturesDoesNotExistError(featuresId: String) extends FeaturesError
case class FeaturesAlreadyExistError(featuresId: String) extends FeaturesError
case class ReferenceFeatureDoesNotExistError() extends FeaturesError
case class UnsupportedDimensionError(featureClass: String, dimension: String)
    extends FeaturesError
case class RecursiveFeatureError(featureClass: String, dimension: String)
    extends FeaturesError
