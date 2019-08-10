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
