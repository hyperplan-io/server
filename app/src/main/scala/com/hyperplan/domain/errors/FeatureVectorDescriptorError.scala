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
