package com.hyperplan.domain.errors

sealed trait DomainError extends Throwable {
  override def getMessage: String
}

case class DomainClassAlreadyExists(domainClassId: String) extends DomainError {
  override def getMessage = s"The domain class $domainClassId already exists"
}

case class DomainClassDataIncorrect(domainClassId: String) extends DomainError {
  override def getMessage = s"The domain class $domainClassId data is incorrect"
}

case class FeaturesClassDoesNotExist(featuresId: String) extends DomainError {
  override def getMessage = s"The features class $featuresId does not exist"
}

case class LabelsClassDoesNotExist(labelsId: String) extends DomainError {
  override def getMessage = s"The labels class $labelsId does not exist"
}
