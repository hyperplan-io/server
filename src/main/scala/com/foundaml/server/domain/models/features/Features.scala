package com.foundaml.server.domain.models.features

sealed trait Features {
  def data: List[Any]
}

case class FloatFeatures(data: List[Float]) extends Features
object FloatFeatures {
  val featuresClass = "FloatVector"
}

case class IntFeatures(data: List[Int]) extends Features
object IntFeatures {
  val featuresClass = "IntVector"
}

case class StringFeatures(data: List[String]) extends Features
object StringFeatures {
  val featuresClass = "StringVector"
}

case class CustomFeatures(data: List[CustomFeature]) extends Features
object CustomFeatures {
  val featuresClass = "CustomFeatures"
}

sealed trait CustomFeature

case class FloatFeature(value: Float) extends CustomFeature
object FloatFeature {
  val featureClass = "Float"
}

case class IntFeature(value: Int) extends CustomFeature
object IntFeature {
  val featureClass = "Int"
}
case class StringFeature(value: String) extends CustomFeature
object StringFeature {
  val featureClass = "String"
}
