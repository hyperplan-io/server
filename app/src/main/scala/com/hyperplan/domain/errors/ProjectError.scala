package com.hyperplan.domain.errors

sealed trait ProjectError extends Exception {
  val message: String
  override def getMessage = message
}
object ProjectError {

  case class ProjectIdIsNotAlphaNumericalError(message: String)
      extends ProjectError
  object ProjectIdIsNotAlphaNumericalError {
    def message(projectId: String) =
      s"Project id $projectId is not alphanumerical"
  }

  case class ProjectIdIsEmptyError() extends ProjectError {
    val message: String = "Empty id is not allowed"
  }

  case class ProjectNameIsEmptyError() extends ProjectError {
    val message: String = "Empty name is not allowed"
  }

  case class FeaturesDoesNotExistError(message: String) extends ProjectError
  object FeaturesDoesNotExistError {
    def message(featuresId: String) = s"The features $featuresId does not exist"
  }

  case class LabelsDoesNotExistError(message: String) extends ProjectError
  object LabelsDoesNotExistError {
    def message(labelsId: String) = s"The labels $labelsId does not exist"
  }

  case class ProjectLabelsAreRequiredForClassificationError()
      extends ProjectError {
    val message = "The project labels are required for classification"
  }

  case class InvalidProjectIdentifierError(message: String) extends ProjectError

  case class FeaturesConfigurationError(message: String) extends ProjectError

  case class ProjectDataInconsistentError(message: String) extends ProjectError

  object ProjectDataInconsistentError {
    def message(projectId: String) =
      s"The project $projectId has inconsistent data"
  }

  case class ProjectAlreadyExistsError(message: String) extends ProjectError
  object ProjectAlreadyExistsError {
    def message(projectId: String) = s"The project $projectId already exists"
  }

  case class ProjectDoesNotExistError(message: String) extends ProjectError
  object ProjectDoesNotExistError {
    def message(projectId: String): String =
      s"The project $projectId does not exist"
  }

  case class ProjectPolicyAlgorithmDoesNotExist(message: String)
      extends ProjectError
  object ProjectPolicyAlgorithmDoesNotExist {
    def message(algorithmId: String): String =
      s"The algorithm  $algorithmId set in the policy does not exist"
    def message(algorithmIds: Seq[String]): String =
      s"The following algorithms set in the policy do not exist: ${algorithmIds.mkString(",")}"
  }

  case class ClassificationProjectRequiresLabelsError(message: String)
      extends ProjectError

  case class RegressionProjectDoesNotRequireLabelsError(message: String)
      extends ProjectError
}
