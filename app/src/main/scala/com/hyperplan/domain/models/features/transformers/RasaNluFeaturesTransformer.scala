/*
 *
 * This file is part of the Hyperplan project.
 * Copyright (C) 2019  Hyperplan
 * Authors: Antoine Sauray
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gn/u.org/licenses/>.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the Hyperplan software without
 * disclosing the source code of your own applications.
 *
 *
 */

package com.hyperplan.domain.models.features.transformers

import cats.implicits._

import com.hyperplan.domain.models.features.Features.Features
import com.hyperplan.domain.models.features._
import com.hyperplan.domain.errors._
import cats.data.NonEmptyList
import scala.collection.immutable.Nil

case class RasaNluFeaturesTransformer(
    field: String,
    joinCharacter: String
) {
  val classes: List[String] = field.split('.').toList

  private def transformRecursively(
      classes: NonEmptyList[String],
      features: Features,
      project: String,
      model: String
  ): Either[RasaFeaturesTransformerError, RasaNluFeatures] =
    features
      .collectFirst {
        case StringFeature(key, value) if key == classes.head =>
          RasaNluFeatures(value, project, model).asRight
        case StringArrayFeature(key, values) if key == classes.head =>
          RasaNluFeatures(values.mkString(joinCharacter), project, model).asRight
        case StringMatrixFeature(key, values) if key == classes.head =>
          RasaNluFeatures(
            values.flatten.mkString(joinCharacter),
            project,
            model
          ).asRight
        case ReferenceFeature(key, reference, value) =>
          classes.tail match {
            case head :: tail =>
              transformRecursively(
                NonEmptyList(head, tail),
                value,
                project,
                model
              )
            case Nil =>
              Left(DidNotFindField(field))
          }
        case feature if feature.key == classes.head =>
          IllegalFieldType(field, feature.getClass.getSimpleName).asLeft
      }
      .getOrElse(Left(DidNotFindField(field)))

  def transform(
      features: Features,
      project: String,
      model: String
  ): Either[RasaFeaturesTransformerError, RasaNluFeatures] =
    classes match {
      case head :: tail =>
        transformRecursively(
          NonEmptyList(head, classes.tail),
          features,
          project,
          model
        )
      case Nil =>
        Left(EmptyFieldNotAllowed(field))

    }

}
