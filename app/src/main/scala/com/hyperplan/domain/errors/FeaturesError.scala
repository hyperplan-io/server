package com.hyperplan.domain.errors

sealed trait FeaturesError

case class FeaturesAlreadyExistError(featuresId: String) extends FeaturesError
case class ReferenceFeatureDoesNotExistError(reference: String)
    extends FeaturesError
case class UnsupportedDimensionError(featureClass: String, dimension: String)
    extends FeaturesError
case class RecursiveFeatureError(featureClass: String, dimension: String)
    extends FeaturesError
