package com.hyperplan.domain.validators

import cats.data._
import cats.effect.IO
import cats.implicits._

import com.hyperplan.domain.errors.AlgorithmError
import com.hyperplan.domain.errors.AlgorithmError._

import com.hyperplan.domain.models._
import com.hyperplan.domain.models.backends._
import com.hyperplan.domain.models.features.transformers.TensorFlowFeaturesTransformer
import com.hyperplan.domain.models.labels.transformers.TensorFlowLabelsTransformer
import io.lemonlabs.uri.AbsoluteUrl

object AlgorithmValidator {

  type AlgorithmValidationResult[A] = ValidatedNec[AlgorithmError, A]

  def validateFeaturesConfiguration(
      expectedSize: Int,
      actualSize: Int,
      featureName: String
  ) =
    if (expectedSize != actualSize) {
      Some(
        AlgorithmError.IncompatibleFeaturesError(
          s"The features dimension is incorrect for the project"
        )
      )
    } else {
      None
    }

  def validateLabelsConfiguration(
      labels: Map[String, String],
      labelsConfiguration: LabelVectorDescriptor
  ) = labelsConfiguration.data match {
    case OneOfLabelsDescriptor(oneOf, _) =>
      if (labels.size != oneOf.size) {
        Some(
          AlgorithmError.IncompatibleLabelsError(
            s"The labels dimension is incorrect for the project"
          )
        )
      } else {
        None
      }
    case DynamicLabelsDescriptor(description) =>
      None
  }

  def validateProtocol(url: String): AlgorithmValidationResult[Protocol] =
    AbsoluteUrl.parse(url).scheme match {
      case "http" =>
        Validated.valid[AlgorithmError, Protocol](Http).toValidatedNec
      case "grpc" =>
        Validated.valid[AlgorithmError, Protocol](Grpc).toValidatedNec
      case scheme =>
        Validated
          .invalid[AlgorithmError, Protocol](
            UnsupportedProtocolError(
              UnsupportedProtocolError.message(scheme)
            )
          )
          .toValidatedNec
    }

  def validateProtocolAndVerifyCompatibility(
      backend: Backend
  ): AlgorithmValidationResult[Protocol] = backend match {
    case LocalRandomClassification(_) =>
      Validated.valid[AlgorithmError, Protocol](LocalCompute).toValidatedNec
    case LocalRandomRegression() =>
      Validated.valid[AlgorithmError, Protocol](LocalCompute).toValidatedNec
    case BasicHttpClassification(_, _) =>
      Validated.valid[AlgorithmError, Protocol](Http).toValidatedNec
    case TensorFlowClassificationBackend(_, _, _, _, _) =>
      Validated.valid[AlgorithmError, Protocol](Http).toValidatedNec
    case TensorFlowRegressionBackend(_, _, _, _) =>
      Validated.valid[AlgorithmError, Protocol](Http).toValidatedNec
    case RasaNluClassificationBackend(rootPath, _, _, _, _) =>
      validateProtocol(rootPath).andThen {
        case protocol @ Http =>
          Validated.valid[AlgorithmError, Protocol](protocol).toValidatedNec
        case protocol @ Grpc =>
          Validated
            .invalid[AlgorithmError, Protocol](
              UnsupportedProtocolError(
                UnsupportedProtocolError.message(protocol)
              )
            )
            .toValidatedNec
        case protocol @ LocalCompute =>
          Validated
            .invalid[AlgorithmError, Protocol](
              UnsupportedProtocolError(
                UnsupportedProtocolError.message(protocol)
              )
            )
            .toValidatedNec
      }
  }

  def validateClassificationAlgorithm(
      algorithm: Algorithm,
      project: ClassificationProject
  ): AlgorithmValidationResult[Unit] = {
    algorithm.backend match {
      case LocalRandomClassification(computed) =>
        Validated.valid[AlgorithmError, Unit](Unit).toValidatedNec
      case LocalRandomRegression() =>
        Validated
          .invalid[AlgorithmError, Unit](
            IncompatibleAlgorithmError(
              IncompatibleAlgorithmError.message(
                algorithm.id,
                algorithm.backend.getClass.getSimpleName,
                project.problem
              )
            )
          )
          .toValidatedNec
      case BasicHttpClassification(_, _) =>
        Validated.valid[AlgorithmError, Unit](Unit).toValidatedNec
      case TensorFlowClassificationBackend(
          _,
          _,
          _,
          TensorFlowFeaturesTransformer(signatureName, fields),
          TensorFlowLabelsTransformer(labels)
          ) =>
        val validatedlabels = project.configuration.labels.data match {
          case OneOfLabelsDescriptor(oneOf, _) =>
            Either
              .cond(
                oneOf.size == labels.size,
                (),
                WrongNumberOfLabelsInTransformerError(
                  WrongNumberOfLabelsInTransformerError.message(
                    labels.size,
                    oneOf.size
                  )
                ): AlgorithmError
              )
              .toValidatedNec
          case DynamicLabelsDescriptor(_) =>
            Validated.valid[AlgorithmError, Unit](Unit).toValidatedNec
        }

        val validatedFeatures = Either
          .cond(
            project.configuration.features.data.size == fields.size,
            (),
            WrongNumberOfFeaturesInTransformerError(
              WrongNumberOfFeaturesInTransformerError.message(
                fields.size,
                project.configuration.features.data.size
              )
            ): AlgorithmError
          )
          .toValidatedNec

        validatedFeatures.andThen(_ => validatedlabels)
      case backend: TensorFlowRegressionBackend =>
        Validated
          .invalid(
            AlgorithmError.IncompatibleAlgorithmError(
              AlgorithmError.IncompatibleAlgorithmError
                .message(
                  algorithm.id,
                  backend.getClass.getSimpleName,
                  project.problem
                )
            )
          )
          .toValidatedNec
      case backend: RasaNluClassificationBackend =>
        Validated.valid[AlgorithmError, Unit](Unit).toValidatedNec
    }

  }

  def validateRegressionAlgorithm(
      algorithm: Algorithm,
      project: RegressionProject
  ): AlgorithmValidationResult[Unit] = {
    algorithm.backend match {
      case backend: LocalRandomClassification =>
        Validated
          .invalid(
            AlgorithmError.IncompatibleAlgorithmError(
              AlgorithmError.IncompatibleAlgorithmError
                .message(
                  algorithm.id,
                  backend.getClass.getSimpleName,
                  project.problem
                )
            )
          )
          .toValidatedNec
      case _: LocalRandomRegression =>
        Validated.valid(()).toValidatedNec
      case backend: BasicHttpClassification =>
        Validated
          .invalid(
            AlgorithmError.IncompatibleAlgorithmError(
              AlgorithmError.IncompatibleAlgorithmError
                .message(
                  algorithm.id,
                  backend.getClass.getSimpleName,
                  project.problem
                )
            )
          )
          .toValidatedNec
      case TensorFlowRegressionBackend(
          _,
          _,
          _,
          TensorFlowFeaturesTransformer(signatureName, fields)
          ) =>
        Either
          .cond(
            project.configuration.features.data.size == fields.size,
            (),
            WrongNumberOfFeaturesInTransformerError(
              WrongNumberOfFeaturesInTransformerError.message(
                fields.size,
                project.configuration.features.data.size
              )
            ): AlgorithmError
          )
          .toValidatedNec
      case backend: TensorFlowClassificationBackend =>
        Validated
          .invalid(
            AlgorithmError.IncompatibleAlgorithmError(
              AlgorithmError.IncompatibleAlgorithmError
                .message(
                  algorithm.id,
                  backend.getClass.getSimpleName,
                  project.problem
                )
            )
          )
          .toValidatedNec

      case backend: RasaNluClassificationBackend =>
        Validated
          .invalid(
            AlgorithmError.IncompatibleAlgorithmError(
              AlgorithmError.IncompatibleAlgorithmError
                .message(
                  algorithm.id,
                  backend.getClass.getSimpleName,
                  project.problem
                )
            )
          )
          .toValidatedNec
    }
  }

  def validateAlphanumericalAlgorithmId(
      id: String
  ): AlgorithmValidationResult[String] =
    Either
      .cond(
        id.matches("^[a-zA-Z0-9]*$"),
        id,
        AlgorithmIdIsNotAlphaNumerical(
          AlgorithmIdIsNotAlphaNumerical.message(id)
        )
      )
      .toValidatedNec

  def validateAlgorithmCreate(
      algorithm: Algorithm,
      project: Project
  ): AlgorithmValidationResult[Protocol] =
    (project match {
      case classificationProject: ClassificationProject =>
        validateClassificationAlgorithm(
          algorithm,
          classificationProject
        )
      case regressionProject: RegressionProject =>
        validateRegressionAlgorithm(algorithm, regressionProject)
    }).andThen(_ => validateAlphanumericalAlgorithmId(algorithm.id))
      .andThen(_ => validateProtocolAndVerifyCompatibility(algorithm.backend))

}
