package com.hyperplan.domain.errors


sealed trait ProjectError extends Exception {
  val message: String
  override def getMessage = message
}
object ProjectError {
  case class ProjectIdIsNotAlphaNumerical(message: String) extends ProjectError
  object ProjectIdIsNotAlphaNumerical {
    def message(projectId: String) =
      s"Project id $projectId is not alphanumerical"
  }

  case class FeaturesDoesNotExistError(message: String) extends ProjectError
  object FeaturesDoesNotExistError {
    def message(featuresId: String) = s"The features $featuresId does not exist"
  }

  case class LabelsDoesNotExistError(message: String) extends ProjectError
  object LabelsDoesNotExistError {
    def message(labelsId: String) = s"The labels $labelsId does not exist"
  }

  case class ProjectLabelsAreRequiredForClassification() extends ProjectError {
    val message = "The project labels are required for classification"
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
}
