package com.hyperplan.domain.models

import cats.implicits._

import com.hyperplan.domain.models.features.Features._
import com.hyperplan.domain.models.features._
import scala.util.Random

/**
  * A feature vector descriptor is a representation of what data the feature vector will hold when instantiated
  * @param id the identifier of the feature vector descriptor
  * @param data the description of the data it will hold when instantiated
  */
case class FeatureVectorDescriptor(id: String, data: List[FeatureDescriptor])
object FeatureVectorDescriptor {

  def randomFloatList(): List[Float] =
    (for (i <- 1 to 10) yield (Random.nextFloat())).toList

  def randomIntList(): List[Int] =
    (for (i <- 1 to 10) yield (Random.nextInt())).toList
  def randomStringList(): List[String] =
    (for (i <- 1 to 10) yield (Random.nextString(10))).toList

  def generateRandomFeatureVector(
      featureVectorDescriptor: FeatureVectorDescriptor
  ): Features =
    featureVectorDescriptor.data.flatMap { descriptor =>
      (descriptor.featuresType, descriptor.dimension) match {
        case (FloatFeatureType, Scalar) =>
          FloatFeature(
            descriptor.name,
            Random.nextFloat()
          ).some
        case (FloatFeatureType, Array) =>
          FloatArrayFeature(
            descriptor.name,
            randomFloatList()
          ).some
        case (FloatFeatureType, Matrix) =>
          FloatMatrixFeature(
            descriptor.name,
            (for (i <- 1 to 5) yield (randomFloatList()).toList).toList
          ).some
        case (IntFeatureType, Scalar) =>
          IntFeature(
            descriptor.name,
            Random.nextInt()
          ).some
        case (IntFeatureType, Array) =>
          IntArrayFeature(
            descriptor.name,
            randomIntList()
          ).some
        case (IntFeatureType, Matrix) =>
          IntMatrixFeature(
            descriptor.name,
            (for (i <- 1 to 5) yield (randomIntList()).toList).toList
          ).some
        case (StringFeatureType, Scalar) =>
          StringFeature(
            descriptor.name,
            Random.nextString(10)
          ).some
        case (StringFeatureType, Array) =>
          StringArrayFeature(
            descriptor.name,
            randomStringList()
          ).some
        case (StringFeatureType, Matrix) =>
          StringMatrixFeature(
            descriptor.name,
            (for (i <- 1 to 5) yield (randomStringList()).toList).toList
          ).some
        case (ReferenceFeatureType(name), Scalar) =>
          none[Feature]
        case (ReferenceFeatureType(name), Array) =>
          none[Feature]
        case (ReferenceFeatureType(name), Matrix) =>
          none[Feature]
      }
    }
}

/**
  * A label vector descriptor is a representation of what data the label vector will hold when instantiated
  * @param id the identifier of the label vector descriptor
  * @param data the description of the data it will hold when instantiated
  */
case class LabelVectorDescriptor(id: String, data: LabelDescriptor)

/**
  * A feature descriptor is a representation of what data the feature will hold when instantiated
  * @param name the name of the feature
  * @param featuresType the type of the feature
  * @param dimension the dimension (scalar, array or matrix)
  * @param description a short description of the feature
  */
case class FeatureDescriptor(
    name: String,
    featuresType: FeatureType,
    dimension: FeatureDimension,
    description: String
) {

  /**
    * Whether or not this feature actually is a pointer to another feature. This is useful to create nested features.
    * @return whether or not this feature is a reference to another feature
    */
  def isReference: Boolean = featuresType match {
    case ReferenceFeatureType(_) => true
    case _ => false
  }
}

/**
  * A label descriptor is a representation of what data the label will hold when instantiated
  * @param description a short description of the label
  */
sealed trait LabelDescriptor {
  def description: String
}

/**
  * A label descriptor of type `oneOf` is useful when the different labels are already known and the cardinality is quite small
  * @param oneOf The different labels possible
  * @param description a short description of the label
  */
case class OneOfLabelsDescriptor(oneOf: Set[String], description: String)
    extends LabelDescriptor
object OneOfLabelsDescriptor {
  val labelsType = "oneOf"
}

/**
  * A label descriptor of type `dynamic` is useful when the different labels are not known or when the cardinality is high.
  * @param description a short description of the label
  */
case class DynamicLabelsDescriptor(description: String) extends LabelDescriptor
object DynamicLabelsDescriptor {
  val labelsType = "dynamic"
}
