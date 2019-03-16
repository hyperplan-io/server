package com.foundaml.server.domain.models.features

sealed trait Features {
  def features: List[Feature]
}

case class DoubleFeatures(features: List[DoubleFeature]) extends Features
object DoubleFeatures {
  val featuresClass = "DoubleFeatures"
}

case class FloatFeatures(features: List[FloatFeature]) extends Features
object FloatFeatures {
  val featuresClass = "FloatFeatures"
}

case class IntFeatures(features: List[IntFeature]) extends Features
object IntFeatures {
  val featuresClass = "IntFeatures"
}

case class StringFeatures(features: List[StringFeature]) extends Features
object StringFeatures {
  val featuresClass = "StringFeatures"
}

case class CustomFeatures(features: List[Feature]) extends Features
object CustomFeatures {
  val featuresClass = "CustomFeatures"
}

sealed trait Feature

case class DoubleFeature(value: Double) extends Feature
case class FloatFeature(value: Float) extends Feature
case class IntFeature(value: Int) extends Feature
case class StringFeature(value: String) extends Feature
