package com.hyperplan.domain.models.features.transformers

import cats.implicits._

import com.hyperplan.domain.models.features.Features.Features
import com.hyperplan.domain.models.features._
import com.hyperplan.domain.models.errors._
import cats.data.NonEmptyList
import scala.collection.immutable.Nil

case class RasaNluFeaturesTransformer(
    field: String,
    joinCharacter: String
) {
  val classes: List[String] = field.split('.').toList

  private def transformRecursively(
      classes: NonEmptyList[String],
      features: Features
  ): Either[RasaFeaturesTransformerError, RasaNluFeatures] =
    features
      .collectFirst {
        case StringFeature(key, value) if key == classes.head =>
          RasaNluFeatures(value).asRight
        case StringVectorFeature(key, values) if key == classes.head =>
          RasaNluFeatures(values.mkString(joinCharacter)).asRight
        case StringVector2dFeature(key, values) if key == classes.head =>
          RasaNluFeatures(values.flatten.mkString(joinCharacter)).asRight
        case ReferenceFeature(key, reference, value) =>
          classes.tail match {
            case head :: tail =>
              transformRecursively(NonEmptyList(head, tail), value)
            case Nil =>
              Left(DidNotFindField(field))
          }
        case feature if feature.key == classes.head =>
          IllegalFieldType(field, feature.getClass.getSimpleName).asLeft
      }
      .getOrElse(Left(DidNotFindField(field)))

  def transform(
      features: Features
  ): Either[RasaFeaturesTransformerError, RasaNluFeatures] =
    classes match {
      case head :: tail =>
        transformRecursively(NonEmptyList(head, classes.tail), features)
      case Nil =>
        Left(EmptyFieldNotAllowed(field))

    }

}
