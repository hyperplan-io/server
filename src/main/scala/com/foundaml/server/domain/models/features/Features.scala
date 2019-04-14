package com.foundaml.server.domain.models.features

object Features {
  type Features = List[Feature]
}

sealed trait Feature

case class FloatVectorFeature(data: List[Float]) extends Feature
object FloatVectorFeature {
  val featureClass = "FloatVector"
}

case class FloatVector2dFeature(data: List[List[Float]]) extends Feature
object FloatVector2dFeature {
  val featureClass = "FloatVector2d"
}
case class IntVectorFeature(data: List[Int]) extends Feature
object IntVectorFeature {
  val featureClass = "IntVector"
}

case object EmptyVectorFeature extends Feature

case class IntVector2dFeature(data: List[List[Int]]) extends Feature
object IntVector2dFeature {
  val featureClass = "IntVector2d"
}

case class StringVectorFeature(data: List[String]) extends Feature
object StringVectorFeature {
  val featureClass = "StringVector"
}

case class StringVector2dFeature(data: List[List[String]]) extends Feature
object StringVector2dFeature {
  val featureClass = "StringVector2d"
}

case object EmptyVector2dFeature extends Feature

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
