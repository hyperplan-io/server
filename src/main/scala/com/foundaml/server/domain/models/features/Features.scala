package com.foundaml.server.domain.models.features

sealed trait Features {
  def features: List[Feature]
}
case class DoubleFeatures(features: List[DoubleFeature]) extends Features
case class FloatFeatures(features: List[FloatFeature]) extends Features
case class IntFeatures(features: List[IntFeature]) extends Features
case class StringFeatures(features: List[StringFeature]) extends Features

sealed trait Feature

case class DoubleFeature(value: Double) extends Feature
case class FloatFeature(value: Float) extends Feature
case class IntFeature(value: Int) extends Feature
case class StringFeature(value: String) extends Feature
