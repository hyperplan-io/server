package com.foundaml.server.domain.models.features

object Features {
  type Features = List[Feature]
}

sealed trait Feature {
  val dimension: FeatureDimension
  val key: String
}

sealed trait FeatureType {
  val name: String
}

case object FloatFeatureType extends FeatureType {
  val name = "Float"
}

case object IntFeatureType extends FeatureType {
  val name = "Int"
}

case object StringFeatureType extends FeatureType {
  val name = "String"
}

case class ReferenceFeatureType(name: String) extends FeatureType

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

case class FloatVectorFeature(key: String, data: List[Float]) extends Feature {
  val dimension = Vector
}

case class FloatVector2dFeature(key: String, data: List[List[Float]]) extends Feature {
  val dimension = Matrix
}

case class IntVectorFeature(key: String, data: List[Int]) extends Feature {
  val dimension = Vector
}

case class EmptyVectorFeature(key: String) extends Feature {
  val dimension = Vector
}

case class IntVector2dFeature(key: String, data: List[List[Int]]) extends Feature {
  val dimension = Matrix
}

case class StringVectorFeature(key: String, data: List[String]) extends Feature {
  val dimension = Vector
}

case class StringVector2dFeature(key: String, data: List[List[String]]) extends Feature {
  val dimension = Matrix
}
case class EmptyVector2dFeature(key: String) extends Feature {
  val dimension = Matrix
}

case class FloatFeature(key: String, value: Float) extends Feature {
  val dimension = One
}
object FloatFeature {
  val featureClass = "Float"
}

case class IntFeature(key: String, value: Int) extends Feature {
  val dimension = One
}
object IntFeature {
  val featureClass = "Int"
}
case class StringFeature(key: String, value: String) extends Feature {
  val dimension = One
}
object StringFeature {
  val featureClass = "String"
}

case class ReferenceFeature(key: String, value: Features.Features) extends Feature {
  val dimension = One
}
object ReferenceFeature {
  val featureClass = "Reference"
}
