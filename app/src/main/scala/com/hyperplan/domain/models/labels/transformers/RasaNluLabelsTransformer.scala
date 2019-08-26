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

import cats.implicits._

import com.hyperplan.domain.models.labels.RasaNluClassificationLabels
import com.hyperplan.domain.models.LabelVectorDescriptor
import com.hyperplan.domain.models.labels.ClassificationLabel
import com.hyperplan.domain.errors._
import com.hyperplan.domain.services.ExampleUrlService
import com.hyperplan.domain.models._

case class RasaNluLabelsTransformer() {

  def transform(
      labelsConfiguration: LabelVectorDescriptor,
      predictionId: String,
      labels: RasaNluClassificationLabels
  ): Either[RasaNluLabelsTransformerError, Set[ClassificationLabel]] =
    labelsConfiguration.data match {
      case OneOfLabelsDescriptor(oneOf, description) =>
        val classificationLabels = labels.intent_ranking.collect {
          case prediction if oneOf.contains(prediction.name) =>
            ClassificationLabel(
              prediction.name,
              prediction.confidence,
              ExampleUrlService.correctClassificationExampleUrl(
                predictionId,
                prediction.name
              ),
              ExampleUrlService.incorrectClassificationExampleUrl(
                predictionId,
                prediction.name
              )
            )
        }.toSet

        if (classificationLabels.size == oneOf.size) {
          classificationLabels.asRight
        } else {
          RasaNluMissingLabelError().asLeft
        }

      case DynamicLabelsDescriptor(_) =>
        labels.intent_ranking
          .map { prediction =>
            ClassificationLabel(
              prediction.name,
              prediction.confidence,
              ExampleUrlService.correctClassificationExampleUrl(
                predictionId,
                prediction.name
              ),
              ExampleUrlService.incorrectClassificationExampleUrl(
                predictionId,
                prediction.name
              )
            )
          }
          .toSet
          .asRight
    }

}
