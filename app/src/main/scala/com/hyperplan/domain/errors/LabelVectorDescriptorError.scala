package com.hyperplan.domain.errors

sealed trait LabelVectorDescriptorError extends Exception {
  val message: String
  override def getMessage = message
}

case class OneOfLabelVectorDescriptorCannotBeEmpty()
    extends LabelVectorDescriptorError {
  val message = "The labels of type oneOf cannot be empty"
}

case class LabelVectorDescriptorAlreadyExist(labelsId: String)
    extends LabelVectorDescriptorError {
  val message = s"The labels $labelsId already exist"
}

case class LabelVectorDescriptorDoesNotExist(labelsId: String)
    extends LabelVectorDescriptorError {
  val message = s"The labels $labelsId does not exist"
}
