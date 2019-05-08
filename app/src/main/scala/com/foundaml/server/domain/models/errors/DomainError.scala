package com.foundaml.server.domain.models.errors

sealed trait DomainError extends Throwable {
  override def getMessage: String
}

case class DomainClassAlreadyExists(domainClassId: String) extends DomainError {
  override def getMessage = s"The domain class $domainClassId already exists"
}

case class DomainClassDataIncorrect(domainClassId: String) extends DomainError {
  override def getMessage = s"The domain class $domainClassId data is incorrect"
}
