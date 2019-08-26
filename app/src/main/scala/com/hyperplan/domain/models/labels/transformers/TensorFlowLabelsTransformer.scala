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

package com.hyperplan.domain.models.labels.transformers

import java.util.UUID

import com.hyperplan.domain.models.{
  DynamicLabelsDescriptor,
  LabelVectorDescriptor,
  OneOfLabelsDescriptor
}
import com.hyperplan.domain.errors._
import com.hyperplan.domain.models.labels._
import com.hyperplan.domain.services.ExampleUrlService

case class TensorFlowLabelsTransformer(fields: Map[String, String]) {

  def transformWithOneOfConfiguration(
      predictionId: String,
      tfLabels: TensorFlowClassificationLabels
  ) = {
    val labelsString = tfLabels.result.map(_.label).toSet
    if (labelsString == fields.keys) {
      val labels: Set[ClassificationLabel] = tfLabels.result.flatMap {
        case TensorFlowClassificationLabel(label, probability) =>
          fields
            .get(label)
            .map { label =>
              val correctUrl =
                ExampleUrlService.correctClassificationExampleUrl(
                  predictionId,
                  label
                )
              val incorrectUrl =
                ExampleUrlService.incorrectClassificationExampleUrl(
                  predictionId,
                  label
                )
              ClassificationLabel(
                label,
                probability,
                correctUrl,
                incorrectUrl
              )
            }

      }.toSet

      if (labels.size == fields.keys.size) {
        Right(labels)
      } else {
        Left(
          LabelsTransformerError(
            "A label could not be mapped from TensorFlow result"
          )
        )
      }
    } else {
      Left(
        LabelsTransformerError(
          "labels do not exactly match with classification results"
        )
      )
    }
  }

  def transformWithDynamicLabelsConfiguration(
      predictionId: String,
      tfLabels: TensorFlowClassificationLabels
  ) = {
    val labels = tfLabels.result.map {
      case TensorFlowClassificationLabel(label, probability) =>
        val correctUrl =
          ExampleUrlService.correctClassificationExampleUrl(
            predictionId,
            label
          )
        val incorrectUrl =
          ExampleUrlService.incorrectClassificationExampleUrl(
            predictionId,
            label
          )
        ClassificationLabel(
          label,
          probability,
          correctUrl,
          incorrectUrl
        )
    }.toSet

    Right(labels)
  }

  def transform(
      labelsConfiguration: LabelVectorDescriptor,
      predictionId: String,
      tfLabels: TensorFlowClassificationLabels
  ): Either[LabelsTransformerError, Set[ClassificationLabel]] = {
    labelsConfiguration.data match {
      case OneOfLabelsDescriptor(_, _) =>
        transformWithOneOfConfiguration(predictionId, tfLabels)
      case DynamicLabelsDescriptor(_) =>
        transformWithDynamicLabelsConfiguration(predictionId, tfLabels)
    }
  }
}

object TensorFlowLabelsTransformer {
  def identity(projectLabels: Set[String]) =
    TensorFlowLabelsTransformer(projectLabels.zip(projectLabels).toMap)
}
