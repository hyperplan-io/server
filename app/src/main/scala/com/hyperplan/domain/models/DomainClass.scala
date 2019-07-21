package com.hyperplan.domain.models

import com.hyperplan.domain.models.features.FeatureDimension
import com.hyperplan.domain.models.features.FeatureType
import com.hyperplan.domain.models.features.FloatFeatureType
import com.hyperplan.domain.models.features.IntFeatureType
import com.hyperplan.domain.models.features.ReferenceFeatureType
import com.hyperplan.domain.models.features.StringFeatureType

case class FeaturesConfiguration(id: String, data: List[FeatureConfiguration])
case class LabelsConfiguration(id: String, data: LabelConfiguration)

case class FeatureConfiguration(
    name: String,
    featuresType: FeatureType,
    dimension: FeatureDimension,
    description: String
) {
  def isReference: Boolean = featuresType match {
    case ReferenceFeatureType(name) => true
    case _ => false
  }
}

sealed trait LabelConfiguration {
  def description: String
}
case class OneOfLabelsConfiguration(oneOf: Set[String], description: String)
    extends LabelConfiguration
object OneOfLabelsConfiguration {
  val labelsType = "oneOf"
}
case class DynamicLabelsConfiguration(description: String)
    extends LabelConfiguration
object DynamicLabelsConfiguration {
  val labelsType = "dynamic"
}
