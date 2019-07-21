package com.hyperplan.domain.errors

sealed trait LabelsError {
  def message: String
}

case class OneOfLabelsCannotBeEmpty() extends LabelsError {
  val message = "The labels of type oneOf cannot be empty"
}

case class LabelsAlreadyExist(labelsId: String) extends LabelsError {
  val message = s"The labels $labelsId already exist"
}

case class LabelsDoesNotExist(labelsId: String) extends LabelsError {
  val message = s"The labels $labelsId does not exist"
}
