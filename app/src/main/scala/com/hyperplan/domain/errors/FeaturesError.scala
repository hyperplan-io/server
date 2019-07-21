package com.hyperplan.domain.errors
import com.hyperplan.domain.models.features.FeatureDimension

sealed trait FeaturesError {
  def message: String
}

case class FeaturesDoesNotExistError(featuresId: String) extends FeaturesError {
  val message = s"The features $featuresId does not exist"
}

case class FeaturesAlreadyExistError(featuresId: String) extends FeaturesError {
  val message = s"The feature $featuresId already exists"
}
case class ReferenceFeatureDoesNotExistError(reference: String)
    extends FeaturesError {
  val message =
    s"The feature $reference does not exist and cannot be referenced"
}
case class UnsupportedDimensionError(
    message: String
) extends FeaturesError
object UnsupportedDimensionError {
  def apply(
      featureClass: String,
      dimension: FeatureDimension
  ): UnsupportedDimensionError =
    UnsupportedDimensionError(
      s"The feature $featureClass cannot be used with dimension $dimension"
    )
}
case class RecursiveFeatureError(message: String) extends FeaturesError
