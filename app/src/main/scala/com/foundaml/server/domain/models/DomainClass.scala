package com.foundaml.server.domain.models

import com.foundaml.server.domain.models.features.FeatureDimension

case class FeaturesConfiguration(id: String, data: List[FeatureConfiguration])
case class LabelsConfiguration(id: String, data: LabelConfiguration)

case class FeatureConfiguration(
    name: String,
    featuresType: String,
    dimension: FeatureDimension,
    description: String
)

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
