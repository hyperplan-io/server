package com.foundaml.server.domain.models.features

object Features {
  type Features = List[Feature]
}

sealed trait Feature {
  val dimension: Dimension
}

sealed trait Dimension
case object One extends Dimension
case object Vector extends Dimension
case object Matrix extends Dimension

case class FloatVectorFeature(data: List[Float]) extends Feature {
  val dimension = Vector
}
object FloatVectorFeature {
  val featureClass = "FloatVector"
}

case class FloatVector2dFeature(data: List[List[Float]]) extends Feature {
  val dimension = Matrix 
}
object FloatVector2dFeature {
  val featureClass = "FloatVector2d"
}
case class IntVectorFeature(data: List[Int]) extends Feature {
  val dimension = Vector
}
object IntVectorFeature {
  val featureClass = "IntVector"
}

case object EmptyVectorFeature extends Feature {
  val dimension = Vector
}

case class IntVector2dFeature(data: List[List[Int]]) extends Feature {
  val dimension = Matrix
}
object IntVector2dFeature {
  val featureClass = "IntVector2d"
}

case class StringVectorFeature(data: List[String]) extends Feature {
  val dimension = Vector
}
object StringVectorFeature {
  val featureClass = "StringVector"
}

case class StringVector2dFeature(data: List[List[String]]) extends Feature {
  val dimension = Matrix
}
object StringVector2dFeature {
  val featureClass = "StringVector2d"
}

case object EmptyVector2dFeature extends Feature {
  val dimension = Matrix
}

case class FloatFeature(value: Float) extends Feature {
  val dimension = One
}
object FloatFeature {
  val featureClass = "Float"
}

case class IntFeature(value: Int) extends Feature {
  val dimension = One
}
object IntFeature {
  val featureClass = "Int"
}
case class StringFeature(value: String) extends Feature {
  val dimension = One
}
object StringFeature {
  val featureClass = "String"
}

case class ReferenceFeature(value: List[Feature]) {
  val dimension = One
}
object ReferenceFeature {
  val featureClass = "Reference"
}
