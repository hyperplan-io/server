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

package com.hyperplan.domain.validators

import cats.data._
import cats.implicits._

import com.hyperplan.application.controllers.requests.PostProjectRequest
import com.hyperplan.domain.errors.ProjectError
import com.hyperplan.domain.errors.ProjectError._
import com.hyperplan.domain.models.{Classification, Regression}

object ProjectValidator {

  type ProjectValidationResult[A] = ValidatedNec[ProjectError, A]

  def validateAlphanumericalProjectId(
      id: String
  ): ProjectValidationResult[String] =
    Either
      .cond(
        id.matches("^[a-zA-Z0-9]*$"),
        id,
        ProjectIdIsNotAlphaNumericalError(
          ProjectIdIsNotAlphaNumericalError.message(id)
        )
      )
      .toValidatedNec

  def validateProjectIdNotEmpty(id: String): ProjectValidationResult[String] =
    Either
      .cond(
        id.nonEmpty,
        id,
        ProjectIdIsEmptyError()
      )
      .toValidatedNec

  def validateProjectNameNotEmpty(id: String): ProjectValidationResult[String] =
    Either
      .cond(
        id.nonEmpty,
        id,
        ProjectNameIsEmptyError()
      )
      .toValidatedNec

  def validateLabels(
      projectRequest: PostProjectRequest
  ): ProjectValidationResult[Unit] = projectRequest.problem match {
    case Classification if projectRequest.labelsId.isEmpty =>
      Validated.invalid(
        NonEmptyChain(ProjectLabelsAreRequiredForClassificationError())
      )
    case Classification if projectRequest.labelsId.isDefined =>
      Validated.valid(Unit)
    case Regression =>
      Validated.valid(Unit)
  }

  def validateCreateProject(
      projectRequest: PostProjectRequest
  ): ProjectValidationResult[Unit] =
    (
      validateAlphanumericalProjectId(projectRequest.id),
      validateProjectIdNotEmpty(projectRequest.id),
      validateProjectNameNotEmpty(projectRequest.name),
      validateLabels(projectRequest)
    ).mapN((_, _, _, _) => Unit)
}
