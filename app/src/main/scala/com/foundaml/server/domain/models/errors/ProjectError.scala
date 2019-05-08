package com.foundaml.server.domain.models.errors

sealed trait ProjectError extends Throwable {
  val message: String
  override def getMessage = message
}

case class InvalidProjectIdentifier(message: String) extends ProjectError
case class FeaturesConfigurationError(message: String) extends ProjectError
case class ProjectDataInconsistent(projectId: String) extends ProjectError {
  val message = s"The project $projectId has inconsistent data"
}
case class ProjectAlreadyExists(projectId: String) extends ProjectError {
  val message = s"The project $projectId already exists"
}
case class ProjectDoesNotExist(projectId: String) extends ProjectError {
  val message = s"The project $projectId does not exist"
}
case class ClassificationProjectRequiresLabels(message: String)
    extends ProjectError
case class RegressionProjectDoesNotRequireLabels(message: String)
    extends ProjectError
