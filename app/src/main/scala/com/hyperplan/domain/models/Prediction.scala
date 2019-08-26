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

package com.hyperplan.domain.models

import com.hyperplan.domain.models.Examples.{
  ClassificationExamples,
  RegressionExamples
}
import com.hyperplan.domain.models.features.Features.Features
import com.hyperplan.domain.models.labels.{
  ClassificationLabel,
  Labels,
  RegressionLabel
}

sealed trait Prediction {
  def id: String
  def projectId: String
  def algorithmId: String
  def features: Features
  def predictionType: ProblemType
}

case class ClassificationPrediction(
    id: String,
    projectId: String,
    algorithmId: String,
    features: Features,
    examples: ClassificationExamples,
    labels: Set[ClassificationLabel]
) extends Prediction {
  override def predictionType: ProblemType = Classification
}
case class RegressionPrediction(
    id: String,
    projectId: String,
    algorithmId: String,
    features: Features,
    examples: RegressionExamples,
    labels: Set[RegressionLabel]
) extends Prediction {
  override def predictionType: ProblemType = Regression
}
