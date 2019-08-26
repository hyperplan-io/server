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

import cats.kernel.Semigroup

sealed trait ProjectConfiguration {
  val dataStream: Option[StreamConfiguration]
}

case class ClassificationConfiguration(
    features: FeatureVectorDescriptor,
    labels: LabelVectorDescriptor,
    dataStream: Option[StreamConfiguration]
) extends ProjectConfiguration

case class RegressionConfiguration(
    features: FeatureVectorDescriptor,
    dataStream: Option[StreamConfiguration]
) extends ProjectConfiguration

case class StreamConfiguration(
    topic: String
)

sealed trait Project {
  val id: String
  val name: String
  val problem: ProblemType
  val algorithms: List[Algorithm]
  val policy: AlgorithmPolicy
  val featuresId: String
  val labelsId: Option[String]
  val configuration: ProjectConfiguration

  lazy val algorithmsMap: Map[String, Algorithm] =
    algorithms.map(algorithm => algorithm.id -> algorithm).toMap
}

object Project {
  implicit val semigroup: Semigroup[Project] = (x: Project, y: Project) =>
    x match {
      case ClassificationProject(id, name, configuration, algorithms, policy) =>
        ClassificationProject(
          id,
          name,
          configuration,
          algorithms ::: y.algorithms,
          policy
        )
      case RegressionProject(id, name, configuration, algorithms, policy) =>
        RegressionProject(
          id,
          name,
          configuration,
          algorithms ::: y.algorithms,
          policy
        )
    }
}

case class ClassificationProject(
    id: String,
    name: String,
    configuration: ClassificationConfiguration,
    algorithms: List[Algorithm],
    policy: AlgorithmPolicy
) extends Project {
  override val problem: ProblemType = Classification
  val featuresId = configuration.features.id
  val labelsId = Some(configuration.labels.id)
}

object ClassificationProject {

  def apply(
      id: String,
      name: String,
      configuration: ClassificationConfiguration,
      algorithms: Option[List[Algorithm]],
      policy: Option[AlgorithmPolicy]
  ): ClassificationProject =
    ClassificationProject(id, name, configuration, Nil, NoAlgorithm())
}

case class RegressionProject(
    id: String,
    name: String,
    configuration: RegressionConfiguration,
    algorithms: List[Algorithm],
    policy: AlgorithmPolicy
) extends Project {
  override val problem: ProblemType = Regression
  val featuresId = configuration.features.id
  val labelsId = None
}

object RegressionProject {

  def apply(
      id: String,
      name: String,
      configuration: RegressionConfiguration,
      algorithms: Option[List[Algorithm]],
      policy: Option[AlgorithmPolicy]
  ): RegressionProject =
    RegressionProject(id, name, configuration, Nil, NoAlgorithm())
}
