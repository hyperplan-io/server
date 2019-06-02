package com.foundaml.server.domain.models.features

object Features {
  type Features = List[Feature]
}

sealed trait Feature {
  val dimension: FeatureDimension
}

sealed trait FeatureDimension {
  val name: String
}
case object One extends FeatureDimension {
  val name = "One"
}
case object Vector extends FeatureDimension {
  val name = "Vector"
}
case object Matrix extends FeatureDimension {
  val name = "Matrix"
}

case class FloatVectorFeature(data: List[Float]) extends Feature {
  val dimension = Vector
}

case class FloatVector2dFeature(data: List[List[Float]]) extends Feature {
  val dimension = Matrix 
}

case class IntVectorFeature(data: List[Int]) extends Feature {
  val dimension = Vector
}

case object EmptyVectorFeature extends Feature {
  val dimension = Vector
}

case class IntVector2dFeature(data: List[List[Int]]) extends Feature {
  val dimension = Matrix
}


case class StringVectorFeature(data: List[String]) extends Feature {
  val dimension = Vector
}

case class StringVector2dFeature(data: List[List[String]]) extends Feature {
  val dimension = Matrix
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

case class ReferenceFeature(value: Features.Features) extends Feature {
  val dimension = One
}
object ReferenceFeature {
  val featureClass = "Reference"
}
