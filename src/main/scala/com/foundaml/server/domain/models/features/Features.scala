package com.foundaml.server.domain.models.features

sealed trait Features {
  def data: List[Any]
}

case class DoubleFeatures(data: List[Double]) extends Features
object DoubleFeatures {
  val featuresClass = "DoubleFeatures"
}

case class FloatFeatures(data: List[Float]) extends Features
object FloatFeatures {
  val featuresClass = "FloatFeatures"
}

case class IntFeatures(data: List[Int]) extends Features
object IntFeatures {
  val featuresClass = "IntFeatures"
}

case class StringFeatures(data: List[String]) extends Features
object StringFeatures {
  val featuresClass = "StringFeatures"
}

case class CustomFeatures(data: List[CustomFeature]) extends Features
object CustomFeatures {
  val featuresClass = "CustomFeatures"
}

sealed trait CustomFeature

case class DoubleFeature(value: Double) extends CustomFeature
object DoubleFeature {
  val featureClass = "DoubleFeature"
}
case class IntFeature(value: Int) extends CustomFeature
object IntFeature {
  val featureClass = "IntFeature"
}
case class StringFeature(value: String) extends CustomFeature
object StringFeature {
  val featureClass = "StringFeature"
}