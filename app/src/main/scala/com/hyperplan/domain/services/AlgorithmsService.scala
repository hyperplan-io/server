package com.hyperplan.domain.services

import cats.data._
import cats.effect.IO
import cats.implicits._

import io.lemonlabs.uri.{AbsoluteUrl, Url}

import com.hyperplan.domain.models._
import com.hyperplan.domain.models.backends.{
  Backend,
  LocalClassification,
  TensorFlowClassificationBackend,
  TensorFlowRegressionBackend
}
import com.hyperplan.domain.errors.AlgorithmError
import com.hyperplan.domain.errors.AlgorithmError._
import com.hyperplan.domain.models.features.transformers.TensorFlowFeaturesTransformer
import com.hyperplan.domain.models.labels.transformers.TensorFlowLabelsTransformer
import com.hyperplan.domain.repositories.{
  AlgorithmsRepository,
  ProjectsRepository
}
import com.hyperplan.domain.services._
import com.hyperplan.infrastructure.logging.IOLogging
import com.hyperplan.domain.models.backends.RasaNluClassificationBackend
import java.{util => ju}
import cats.effect.ContextShift

class AlgorithmsService(
    projectsService: ProjectsService,
    predictionsService: PredictionsService,
    algorithmsRepository: AlgorithmsRepository,
    projectsRepository: ProjectsRepository
)(implicit cs: ContextShift[IO])
    extends IOLogging {

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
    case LocalClassification(_) =>
      Validated.valid[AlgorithmError, Protocol](LocalCompute).toValidatedNec
    case TensorFlowClassificationBackend(_, _, _, _) =>
      Validated.valid[AlgorithmError, Protocol](Http).toValidatedNec
    case TensorFlowRegressionBackend(_, _, _) =>
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
      case LocalClassification(computed) =>
        Validated.valid[AlgorithmError, Unit](Unit).toValidatedNec
      case TensorFlowClassificationBackend(
          _,
          _,
          TensorFlowFeaturesTransformer(signatureName, fields),
          TensorFlowLabelsTransformer(labels)
          ) =>
        val validatedlabels = project.configuration.labels.data match {
          case OneOfLabelsDescriptor(oneOf, _) =>
            Either
              .cond(
                oneOf.size == fields.size,
                (),
                WrongNumberOfLabelsInTransformerError(
                  WrongNumberOfLabelsInTransformerError.message(
                    fields.size,
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
      case backend: LocalClassification =>
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

  def createAlgorithm(
      id: String,
      backend: Backend,
      projectId: String,
      security: SecurityConfiguration
  ): EitherT[IO, NonEmptyChain[AlgorithmError], Algorithm] = {
    for {
      algorithm <- EitherT.rightT[IO, NonEmptyChain[AlgorithmError]](
        Algorithm(
          id,
          backend,
          projectId,
          security
        )
      )
      project <- EitherT
        .fromOptionF[IO, NonEmptyChain[AlgorithmError], Project](
          projectsService.readProject(projectId),
          NonEmptyChain(
            AlgorithmError.ProjectDoesNotExistError(
              AlgorithmError.ProjectDoesNotExistError.message(projectId)
            ): AlgorithmError
          )
        )
      _ <- EitherT.fromEither[IO](
        validateAlgorithmCreate(algorithm, project).toEither
      )
      _ <- EitherT[IO, NonEmptyChain[AlgorithmError], Prediction](
        predictionsService
          .predictWithBackend(
            ju.UUID.randomUUID().toString,
            project,
            algorithm,
            project.configuration match {
              case ClassificationConfiguration(features, labels, dataStream) =>
                FeatureVectorDescriptor.generateRandomFeatureVector(features)
              case RegressionConfiguration(features, dataStream) =>
                FeatureVectorDescriptor.generateRandomFeatureVector(features)
            }
          )
          .flatMap {
            case Right(prediction) => IO.pure(prediction.asRight)
            case Left(err) =>
              logger.warn(
                s"The prediction dry run failed when creating algorithm ${algorithm.id} because ${err.message}"
              ) *> IO(
                NonEmptyChain(
                  PredictionDryRunFailed(PredictionDryRunFailed.message(err))
                ).asLeft
              )
          }
      )
      _ <- EitherT[IO, NonEmptyChain[AlgorithmError], Algorithm](
        algorithmsRepository.insert(algorithm).map {
          case Right(alg) => alg.asRight
          case Left(error) => NonEmptyChain(error).asLeft
        }
      )
      _ <- if (project.algorithms.isEmpty) {
        (project match {
          case _: ClassificationProject =>
            projectsService
              .updateProject(
                projectId,
                None,
                Some(DefaultAlgorithm(id))
              )
          case _: RegressionProject =>
            projectsService
              .updateProject(
                projectId,
                None,
                Some(DefaultAlgorithm(id))
              )
        }).leftFlatMap[Project, NonEmptyChain[AlgorithmError]](
          _ => EitherT.rightT[IO, NonEmptyChain[AlgorithmError]](project)
        )
      } else {
        EitherT.rightT[IO, NonEmptyChain[AlgorithmError]](project)
      }
    } yield algorithm
  }
}
