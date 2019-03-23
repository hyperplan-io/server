package com.foundaml.server.domain.models.features

case class Features(data: List[Feature])


sealed trait Feature

case class FloatFeatures(data: List[Float]) extends Feature
object FloatFeatures {
  val featuresClass = "FloatVector"
}

case class IntFeatures(data: List[Int]) extends Feature
object IntFeatures {
  val featuresClass = "IntVector"
}

case class StringFeatures(data: List[String]) extends Feature
object StringFeatures {
  val featuresClass = "StringVector"
}

case class FloatFeature(value: Float) extends Feature
object FloatFeature {
  val featureClass = "Float"
}

case class IntFeature(value: Int) extends Feature
object IntFeature {
  val featureClass = "Int"
}
case class StringFeature(value: String) extends Feature
object StringFeature {
  val featureClass = "String"
}
