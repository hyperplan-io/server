package com.foundaml.server.domain.services

import com.foundaml.server.domain.factories.ProjectFactory
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.errors._
import com.foundaml.server.domain.models.features._
import com.foundaml.server.domain.repositories.ProjectsRepository
import com.foundaml.server.infrastructure.logging.IOLogging
import doobie.util.invariant.UnexpectedEnd

import cats.effect.IO
import cats.implicits._

class ProjectsService(
    projectsRepository: ProjectsRepository,
    projectFactory: ProjectFactory
) extends IOLogging {

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

  def validateFeatureClasses(
      featuresConfiguration: FeaturesConfiguration
  ): List[ProjectError] =
    featuresConfiguration match {
      case FeaturesConfiguration(featureConfigurations) =>
        val allowedFeatureClasses = List(
          FloatFeature.featureClass,
          IntFeature.featureClass,
          StringFeature.featureClass,
          FloatVectorFeature.featureClass,
          IntVectorFeature.featureClass,
          StringVectorFeature.featureClass,
          FloatVector2dFeature.featureClass,
          IntVector2dFeature.featureClass,
          StringVector2dFeature.featureClass
        )

        featureConfigurations.flatMap { featureConfiguration =>
          if (allowedFeatureClasses.contains(featureConfiguration.featuresType)) {
            None
          } else {
            Some(
              FeaturesConfigurationError(
                s"${featureConfiguration.featuresType} is not an accepted type for feature ${featureConfiguration.name}"
              )
            )
          }
        }
    }

  def validateClassificationConfiguration(
      configuration: ClassificationConfiguration
  ): List[ProjectError] = {
    validateFeatureClasses(configuration.features)
  }

  def createEmptyProject(
      id: String,
      name: String,
      configuration: ProjectConfiguration
  ): IO[Project] = {
    val project = configuration match {
      case classificationConfiguration: ClassificationConfiguration =>
        ClassificationProject(
          id,
          name,
          classificationConfiguration,
          Nil,
          NoAlgorithm()
        )
      case regressionConfiguration: RegressionConfiguration =>
        RegressionProject(
          id,
          name,
          regressionConfiguration,
          Nil,
          NoAlgorithm()
        )
    }

    val errors = project match {
      case classificationProject: ClassificationProject =>
        validateClassificationConfiguration(classificationProject.configuration)
      case _ => Nil
    }

    for {
      _ <- errors.headOption.fold[IO[Unit]](
        IO.pure(Unit)
      )(
        err => logger.warn(err.getMessage) *> IO.raiseError(err)
      )
      insertResult <- projectsRepository.insert(project)
      result <- insertResult.fold(
        err => logger.warn(err.getMessage) *> IO.raiseError(err),
        _ => IO.pure(project)
      )
    } yield result
  }

  def readProject(id: String) =
    projectFactory.get(id).handleErrorWith {
      case UnexpectedEnd => IO.raiseError(ProjectDoesNotExist(id))
    }

  def updateProject(project: Project) = projectsRepository.update(project)
}
