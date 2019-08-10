package com.hyperplan.domain.services

import java.util.UUID

import cats.effect.IO
import cats.implicits._
import cats.data._
import cats.effect.ContextShift
import doobie.free.connection.{AsyncConnectionIO, ConnectionIO}
import scalacache.Cache
import scalacache.CatsEffect.modes._
import com.hyperplan.infrastructure.logging.IOLogging
import com.hyperplan.application.controllers.requests.PostProjectRequest
import com.hyperplan.domain.models.{backends, _}
import com.hyperplan.domain.models.backends._
import com.hyperplan.domain.errors.AlgorithmError.PredictionDryRunFailed
import com.hyperplan.domain.errors.{AlgorithmError, ProjectError}
import com.hyperplan.domain.errors.ProjectError._
import com.hyperplan.domain.repositories._
import com.hyperplan.domain.repositories.ProjectsRepository
import com.hyperplan.domain.validators.AlgorithmValidator
import com.hyperplan.domain.validators.ProjectValidator._

class ProjectsService(
    val projectsRepository: ProjectsRepository,
    val domainService: DomainService,
    val backendService: BackendService,
    val cache: Cache[Project]
)(implicit val cs: ContextShift[IO])
    extends IOLogging {

  def createClassificationProject(
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
      algorithmId = ProjectsService.defaultRandomAlgorithmId
      (algorithm, policy) = labels.data match {
        case DynamicLabelsDescriptor(_) =>
          Algorithm(
            algorithmId,
            LocalRandomClassification(Set.empty),
            projectRequest.id,
            PlainSecurityConfiguration(Nil)
          ) -> DefaultAlgorithm(algorithmId)
        case OneOfLabelsDescriptor(oneOf, _) =>
          Algorithm(
            algorithmId,
            LocalRandomClassification(oneOf),
            projectRequest.id,
            PlainSecurityConfiguration(Nil)
          ) -> DefaultAlgorithm(algorithmId)
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
        List(algorithm),
        policy
      )

  def createRegressionProject(
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
      algorithmId = ProjectsService.defaultRandomAlgorithmId
      (algorithm, policy) = Algorithm(
        algorithmId,
        LocalRandomRegression(),
        projectRequest.id,
        PlainSecurityConfiguration(Nil)
      ) -> DefaultAlgorithm(algorithmId)
    } yield
      RegressionProject(
        projectRequest.id,
        projectRequest.name,
        RegressionConfiguration(
          features,
          streamConfiguration
        ),
        List(algorithm),
        policy
      )

  def createProject(
      projectRequest: PostProjectRequest
  ): EitherT[IO, NonEmptyChain[ProjectError], Project] =
    (projectRequest.problem match {
      case Classification => createClassificationProject(projectRequest)
      case Regression => createRegressionProject(projectRequest)
    }).flatMap { project =>
      EitherT
        .liftF[IO, NonEmptyChain[ProjectError], Project](
          projectsRepository.transact(
            projectsRepository.insertProject(project)
          )
        )
        .flatMap { _ =>
          EitherT.liftF[IO, NonEmptyChain[ProjectError], Project](
            cache.remove[IO](project.id).map(_ => project)
          )
        }
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
                .readProject(projectId)
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
                      projectsRepository.updateProject(project) *> project
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
    projectsRepository.transact(projectsRepository.readAllProjects)

  def readProject(id: String): IO[Option[Project]] =
    cache.get[IO](id).flatMap { cacheElement =>
      cacheElement.fold(
        projectsRepository.transact(projectsRepository.readProject(id))
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
        projectsRepository
          .transact(projectsRepository.insertAlgorithm(algorithm))
          .map { algorithm =>
            algorithm.asRight
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

object ProjectsService {
  val defaultRandomAlgorithmId = "random"
}
