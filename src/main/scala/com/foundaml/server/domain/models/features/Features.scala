package com.foundaml.server.domain.models.features

object Features {
  type Features = List[Feature]
}

sealed trait Feature

case class FloatVectorFeature(data: List[Float]) extends Feature
object FloatVectorFeature {
  val featureClass = "FloatVector"
}

case class IntVectorFeature(data: List[Int]) extends Feature
object IntVectorFeature {
  val featureClass = "IntVector"
}

case class StringVectorFeature(data: List[String]) extends Feature
object StringVectorFeature {
  val featureClass = "StringVector"
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
