package com.hyperplan.domain.services

import java.util.UUID

import cats.effect.IO
import cats.implicits._
import cats.data._
import com.hyperplan.application.controllers.requests.PostProjectRequest
import com.hyperplan.domain.models._
import com.hyperplan.domain.errors.{AlgorithmError, ProjectError}
import com.hyperplan.domain.errors.ProjectError._
import com.hyperplan.domain.models.features._
import com.hyperplan.domain.repositories.ProjectsRepository
import com.hyperplan.infrastructure.logging.IOLogging
import doobie.free.connection.{AsyncConnectionIO, ConnectionIO}
import doobie.util.invariant.UnexpectedEnd
import scalacache.Cache
import scalacache.CatsEffect.modes._
import com.hyperplan.domain.models.backends.{Backend, LocalClassification}
import com.hyperplan.domain.models.labels.ClassificationLabel
import com.hyperplan.domain.repositories._
import cats.effect.Resource
import org.http4s.client.Client
import cats.effect.ContextShift
import com.hyperplan.domain.errors.AlgorithmError.PredictionDryRunFailed
import com.hyperplan.domain.models
import com.hyperplan.domain.validators.AlgorithmValidator
import com.hyperplan.domain.validators.ProjectValidator._

class ProjectsService(
    val projectsRepository: ProjectsRepository,
    val algorithmsRepository: AlgorithmsRepository,
    val domainService: DomainService,
    val backendService: BackendService,
    val cache: Cache[Project]
)(implicit val cs: ContextShift[IO])
    extends IOLogging {

  def createEmptyClassificationProject(
      projectRequest: PostProjectRequest
  ): EitherT[IO, NonEmptyChain[ProjectError], Project] =
    for {
      _ <- EitherT.fromEither[IO](
        validateCreateProject(projectRequest).toEither
      )
      streamConfiguration <- EitherT.pure[IO, NonEmptyChain[ProjectError]](
        projectRequest.topic.map(topic => StreamConfiguration(topic))
      )
      features <- EitherT
        .fromOptionF[IO, NonEmptyChain[ProjectError], FeatureVectorDescriptor](
          domainService.readFeatures(projectRequest.featuresId),
          NonEmptyChain(
            ProjectError.FeaturesDoesNotExistError(
              ProjectError.FeaturesDoesNotExistError.message(
                projectRequest.featuresId
              )
            )
          )
        )
      labels <- EitherT
        .fromOptionF[IO, NonEmptyChain[ProjectError], LabelVectorDescriptor](
          domainService.readLabels(projectRequest.labelsId.getOrElse("")),
          NonEmptyChain(
            ProjectError.LabelsDoesNotExistError(
              ProjectError.LabelsDoesNotExistError.message(
                projectRequest.labelsId.getOrElse("")
              )
            )
          )
        )
      (algorithm, policy) = labels.data match {
        case DynamicLabelsDescriptor(description) =>
          none[Algorithm] -> NoAlgorithm()
        case OneOfLabelsDescriptor(oneOf, description) =>
          val algorithmId = s"${projectRequest.id}Random"
          Algorithm(
            algorithmId,
            LocalClassification(oneOf),
            projectRequest.id,
            PlainSecurityConfiguration(Nil)
          ).some -> DefaultAlgorithm(algorithmId)
      }
    } yield
      ClassificationProject(
        projectRequest.id,
        projectRequest.name,
        ClassificationConfiguration(
          features,
          labels,
          streamConfiguration
        ),
        List(algorithm).flatten,
        policy
      )

  def createEmptyRegressionProject(
      projectRequest: PostProjectRequest
  ): EitherT[IO, NonEmptyChain[ProjectError], Project] =
    for {
      _ <- EitherT.fromEither[IO](
        validateCreateProject(projectRequest).toEither
      )
      streamConfiguration <- EitherT.pure[IO, NonEmptyChain[ProjectError]](
        projectRequest.topic.map(topic => StreamConfiguration(topic))
      )
      features <- EitherT
        .fromOptionF[IO, NonEmptyChain[ProjectError], FeatureVectorDescriptor](
          domainService.readFeatures(projectRequest.featuresId),
          NonEmptyChain(
            ProjectError.FeaturesDoesNotExistError(
              ProjectError.FeaturesDoesNotExistError.message(
                projectRequest.featuresId
              )
            )
          )
        )
    } yield
      RegressionProject(
        projectRequest.id,
        projectRequest.name,
        RegressionConfiguration(
          features,
          streamConfiguration
        ),
        Nil,
        NoAlgorithm()
      )

  def createEmptyProject(
      projectRequest: PostProjectRequest
  ): EitherT[IO, NonEmptyChain[ProjectError], Project] =
    (projectRequest.problem match {
      case Classification => createEmptyClassificationProject(projectRequest)
      case Regression => createEmptyRegressionProject(projectRequest)
    }).flatMap { project =>
        EitherT(projectsRepository.insert(project).map {
          case Left(err) =>
            // we need a NonEmptyChain of errors but insert returns a single error
            NonEmptyChain(err).asLeft
          case Right(count) => count.asRight
        }).flatMap {
          case insertedCount if insertedCount > 0 =>
            EitherT.liftF[IO, NonEmptyChain[ProjectError], Project](
              cache.remove[IO](project.id).map(_ => project)
            )
          case insertedCount if insertedCount <= 0 =>
            EitherT.liftF[IO, NonEmptyChain[ProjectError], Project](
              IO.raiseError(
                new Exception(
                  "IO returned successfully but nothing was inserted in the database"
                )
              )
            )
        }
      }
      .flatMap { project =>
        project.algorithms.headOption.fold(
          EitherT.rightT[IO, NonEmptyChain[ProjectError]](project)
        )(
          algorithm =>
            EitherT.liftF[IO, NonEmptyChain[ProjectError], Project](
              algorithmsRepository.insert(algorithm).flatMap {
                case Left(err) =>
                  logger.warn("Failed to add a default algorithm", err) *> IO
                    .pure(project)
                case Right(_) =>
                  IO.pure(project)
              }
            )
        )
      }

  def updateProject(
      projectId: String,
      name: Option[String],
      policy: Option[AlgorithmPolicy]
  ): EitherT[IO, NonEmptyChain[ProjectError], Project] =
    EitherT
      .fromEither[IO](
        name
          .fold[ProjectValidationResult[String]](
            Validated.valid("").toValidatedNec
          )(validateProjectNameNotEmpty)
          .toEither
      )
      .flatMap { _ =>
        EitherT(
          projectsRepository
            .transact(
              projectsRepository
                .read(projectId)
                .flatMap[Project] {
                  case Some(project: ClassificationProject) =>
                    (project.copy(
                      name = name.getOrElse(project.name),
                      policy = policy.getOrElse(project.policy)
                    ): Project).pure[ConnectionIO]
                  case Some(project: RegressionProject) =>
                    (project.copy(
                      name = name.getOrElse(project.name),
                      policy = policy.getOrElse(project.policy)
                    ): Project).pure[ConnectionIO]
                  case None =>
                    AsyncConnectionIO.raiseError(
                      ProjectDoesNotExistError(
                        ProjectDoesNotExistError.message(projectId)
                      )
                    )
                }
                .flatMap { project =>
                  val validated = project.policy match {
                    case NoAlgorithm() => Validated.valid(Unit)
                    case DefaultAlgorithm(algorithmId) =>
                      Either
                        .cond(
                          project.algorithms.map(_.id).contains(algorithmId),
                          Unit,
                          ProjectPolicyAlgorithmDoesNotExist(
                            ProjectPolicyAlgorithmDoesNotExist
                              .message(algorithmId)
                          )
                        )
                        .toValidatedNec
                    case WeightedAlgorithm(weights) =>
                      val projectAlgorithmIds = project.algorithms.map(_.id)
                      val algorithmsMissing: Seq[String] =
                        weights.map(_.algorithmId).collect {
                          case id if !projectAlgorithmIds.contains(id) => id
                        }
                      Either
                        .cond(
                          algorithmsMissing.isEmpty,
                          Unit,
                          ProjectPolicyAlgorithmDoesNotExist(
                            ProjectPolicyAlgorithmDoesNotExist
                              .message(algorithmsMissing)
                          )
                        )
                        .toValidatedNec
                  }
                  validated.fold[ConnectionIO[Project]](
                    err => AsyncConnectionIO.raiseError(err.head),
                    _ =>
                      projectsRepository.update(project) *> project
                        .pure[ConnectionIO]
                  )

                }
            )
            .flatMap { project =>
              cache.remove[IO](projectId).map(_ => project.asRight)
            }
            .handleErrorWith {
              case err: ProjectDoesNotExistError =>
                IO.pure(NonEmptyChain(err).asLeft)
              case err: ProjectPolicyAlgorithmDoesNotExist =>
                IO.pure(NonEmptyChain(err).asLeft)
              case err =>
                IO.raiseError(err)
            }
        )

      }

  def readProjects =
    projectsRepository.transact(projectsRepository.readAll)

  def readProject(id: String): IO[Option[Project]] =
    cache.get[IO](id).flatMap { cacheElement =>
      cacheElement.fold(
        projectsRepository.transact(projectsRepository.read(id))
      )(
        project => IO.pure(project.some)
      )
    }

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
          readProject(projectId),
          NonEmptyChain(
            AlgorithmError.ProjectDoesNotExistError(
              AlgorithmError.ProjectDoesNotExistError.message(projectId)
            ): AlgorithmError
          )
        )
      _ <- EitherT.fromEither[IO](
        AlgorithmValidator.validateAlgorithmCreate(algorithm, project).toEither
      )
      _ <- EitherT[IO, NonEmptyChain[AlgorithmError], Prediction](
        backendService
          .predictWithBackend(
            UUID.randomUUID().toString,
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
            updateProject(
              projectId,
              None,
              Some(DefaultAlgorithm(id))
            )
          case _: RegressionProject =>
            updateProject(
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
