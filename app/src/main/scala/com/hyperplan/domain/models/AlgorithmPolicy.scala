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

import scala.util.Random

sealed trait AlgorithmPolicy {
  def take(): Option[String]
  def name: String
}
case class NoAlgorithm() extends AlgorithmPolicy {
  override def take(): Option[String] = None
  val name = NoAlgorithm.name
}

object NoAlgorithm {
  val name = "NoAlgorithm"
}

case class DefaultAlgorithm(algorithmId: String) extends AlgorithmPolicy {
  override def take(): Option[String] = Some(algorithmId)
  val name = DefaultAlgorithm.name
}

object DefaultAlgorithm {
  val name = "DefaultAlgorithm"
}

case class AlgorithmWeight(algorithmId: String, weight: Float)
case class WeightedAlgorithm(weights: List[AlgorithmWeight])
    extends AlgorithmPolicy {

  val cumulativeWeights = weights.foldLeft[List[Float]](Nil) {
    case (acc, tuple) =>
      acc :+ (acc.lastOption.getOrElse(0.0f) + tuple.weight)
  }
  val weightsZipCumulativeWeights = weights.zip(cumulativeWeights)

  override def take(): Option[String] = {
    val rand = Random.nextFloat() * cumulativeWeights.lastOption.getOrElse(0f)
    weightsZipCumulativeWeights
      .foldLeft[List[AlgorithmWeight]](Nil) {
        case (acc, tuple) =>
          val (algorithmWeight, cumWeight) = tuple
          if (cumWeight > rand) {
            acc :+ algorithmWeight
          } else {
            acc
          }
      }
      .headOption
      .map(_.algorithmId)
  }
  val name = WeightedAlgorithm.name
}

object WeightedAlgorithm {
  val name = "WeightedAlgorithm"
}
