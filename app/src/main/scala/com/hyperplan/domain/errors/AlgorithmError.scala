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

package com.hyperplan.domain.errors

import com.hyperplan.domain.models.{ProblemType, Protocol}

sealed trait AlgorithmError extends Exception {
  val message: String
  override def getMessage: String = message
}
object AlgorithmError {

  case class AlgorithmIdIsNotAlphaNumerical(message: String)
      extends AlgorithmError
  object AlgorithmIdIsNotAlphaNumerical {
    def message(algorithmId: String): String =
      s"The algorithm id $algorithmId is not alpha numerical"
  }
  case class ProjectDoesNotExistError(message: String) extends AlgorithmError
  object ProjectDoesNotExistError {
    def message(projectId: String): String =
      s"The project $projectId does not exist"
  }
  case class WrongNumberOfFeaturesInTransformerError(message: String)
      extends AlgorithmError
  object WrongNumberOfFeaturesInTransformerError {
    def message(actualSize: Int, expectedSize: Int) =
      s"Expected $expectedSize fields in feature transformer, got $actualSize"
  }
  case class WrongNumberOfLabelsInTransformerError(message: String)
      extends AlgorithmError
  object WrongNumberOfLabelsInTransformerError {
    def message(actualSize: Int, expectedSize: Int) =
      s"Expected $expectedSize fields in labels transformer, got $actualSize"
  }
  case class IncompatibleFeaturesError(message: String) extends AlgorithmError
  case class IncompatibleLabelsError(message: String) extends AlgorithmError
  case class AlgorithmAlreadyExistsError(message: String) extends AlgorithmError
  object AlgorithmAlreadyExistsError {
    def message(algorithmId: String) =
      s"The algorithm $algorithmId already exists"
  }
  case class IncompatibleAlgorithmError(message: String) extends AlgorithmError
  object IncompatibleAlgorithmError {
    def message(
        algorithmId: String,
        backendClass: String,
        problemType: ProblemType
    ): String =
      s"The algorithm $algorithmId is not compatible. Class is $backendClass and type is ${problemType.problemType}"
  }

  case class AlgorithmDataIsIncorrectError(message: String)
      extends AlgorithmError
  object AlgorithmDataIsIncorrectError {
    def message(algorithmId: String) =
      s"The algorithm $algorithmId data is incorrect"
  }

  case class UnsupportedProtocolError(message: String) extends AlgorithmError
  object UnsupportedProtocolError {
    def message(protocol: String) = s"The protocol $protocol is not supported"
    def message(protocol: Protocol) = s"The protocol $protocol is not supported"
  }

  case class PredictionDryRunFailed(message: String) extends AlgorithmError
  object PredictionDryRunFailed {
    def message(err: PredictionError) =
      s"The prediction dry run failed because ${err.message}"
  }

}
