package com.hyperplan.domain.errors

import com.hyperplan.domain.models.Protocol

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
    def message(algorithmId: String, backendClass: String): String =
      s"The algorithm $algorithmId is not compatible. Class is $backendClass"
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

}
