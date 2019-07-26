package com.hyperplan.domain.services

import cats.effect.IO
import cats.implicits._
import cats.data._

import com.hyperplan.domain.repositories.DomainRepository
import com.foundaml.server.controllers.requests.PostProjectRequest
import com.hyperplan.domain.models._
import com.hyperplan.domain.errors.ProjectError
import com.hyperplan.domain.errors.ProjectError._
import com.hyperplan.domain.models.features._
import com.hyperplan.domain.repositories.ProjectsRepository
import com.hyperplan.infrastructure.logging.IOLogging
import doobie.util.invariant.UnexpectedEnd

import scalacache.Cache
import scalacache.CatsEffect.modes._

class ProjectsService(
    projectsRepository: ProjectsRepository,
    domainService: DomainService,
    cache: Cache[Project]
) extends IOLogging {

  type ProjectValidationResult[A] = ValidatedNec[ProjectError, A]

  val regex = "[0-9a-zA-Z-_]*"
  def validateAlphaNumerical(input: String): List[ProjectError] = {
    if (input.matches(regex)) {
      Nil
    } else {
      List(
        InvalidProjectIdentifier(
          s"$input is not an alphanumerical id. It should satisfy the following regular expression: $regex"
        )
      )
    }
  }

  def createEmptyClassificationProject(
      projectRequest: PostProjectRequest
  ): EitherT[IO, NonEmptyChain[ProjectError], Project] =
    for {
      _ <- EitherT.fromEither[IO](validateProject(projectRequest).toEither)
      streamConfiguration <- EitherT.pure[IO, NonEmptyChain[ProjectError]](
        projectRequest.topic.map(topic => StreamConfiguration(topic))
      )
      features <- EitherT
        .fromOptionF[IO, NonEmptyChain[ProjectError], FeaturesConfiguration](
          domainService.readFeatures(projectRequest.featuresId),
          NonEmptyChain(
            ProjectError.FeaturesDoesNotExistError(projectRequest.featuresId)
          )
        )
      labels <- EitherT
        .fromOptionF[IO, NonEmptyChain[ProjectError], LabelsConfiguration](
          domainService.readLabels(projectRequest.labelsId.getOrElse("")),
          NonEmptyChain(
            ProjectError.LabelsDoesNotExistError(
              projectRequest.labelsId.getOrElse("")
            )
          )
        )
    } yield
      ClassificationProject(
        projectRequest.id,
        projectRequest.name,
        ClassificationConfiguration(
          features,
          labels,
          streamConfiguration
        ),
        Nil,
        NoAlgorithm()
      )

  def validateProjectId(id: String): ProjectValidationResult[String] =
    Either
      .cond(
        id.matches("^[a-zA-Z0-9]*$"),
        id,
        ProjectIdIsNotAlphaNumerical(ProjectIdIsNotAlphaNumerical.message(id))
      )
      .toValidatedNec

  def validateLabels(
      projectRequest: PostProjectRequest
  ): ProjectValidationResult[Unit] = projectRequest.problem match {
    case Classification if projectRequest.labelsId.isEmpty =>
      Validated.invalid(
        NonEmptyChain(ProjectLabelsAreRequiredForClassification())
      )
    case Classification if projectRequest.labelsId.isDefined =>
      Validated.valid(Unit)
    case Regression =>
      Validated.valid(Unit)
  }

  def validateProject(
      projectRequest: PostProjectRequest
  ): ProjectValidationResult[Unit] =
    (
      validateProjectId(projectRequest.id),
      validateLabels(projectRequest)
    ).mapN((_, _) => Unit)

  def createEmptyRegressionProject(
      projectRequest: PostProjectRequest
  ): EitherT[IO, NonEmptyChain[ProjectError], Project] =
    for {
      _ <- EitherT.fromEither[IO](validateProject(projectRequest).toEither)
      streamConfiguration <- EitherT.pure[IO, NonEmptyChain[ProjectError]](
        projectRequest.topic.map(topic => StreamConfiguration(topic))
      )
      features <- EitherT
        .fromOptionF[IO, NonEmptyChain[ProjectError], FeaturesConfiguration](
          domainService.readFeatures(projectRequest.featuresId),
          NonEmptyChain(
            ProjectError.FeaturesDoesNotExistError(projectRequest.featuresId)
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

  def updateProject(
      projectId: String,
      name: Option[String],
      policy: Option[AlgorithmPolicy]
  ): IO[Int] =
    projectsRepository
      .transact(
        projectsRepository
          .read(projectId)
          .map {
            case project: ClassificationProject =>
              project.copy(
                name = name.getOrElse(project.name),
                policy = policy.getOrElse(project.policy)
              )
            case project: RegressionProject =>
              project.copy(
                name = name.getOrElse(project.name),
                policy = policy.getOrElse(project.policy)
              )
          }
          .flatMap { project =>
            projectsRepository.update(project)
          }
      )
      .flatMap { count =>
        cache.remove[IO](projectId).map(_ => count)
      }

  def readProjects =
    projectsRepository.transact(projectsRepository.readAll)

  def readProject(id: String): IO[Project] =
    cache.get[IO](id).flatMap { cacheElement =>
      cacheElement.fold(
        projectsRepository.transact(projectsRepository.read(id))
      )(
        project => IO.pure(project)
      )
    }

}
