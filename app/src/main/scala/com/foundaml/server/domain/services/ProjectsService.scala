package com.foundaml.server.domain.services

import com.foundaml.server.domain.factories.ProjectFactory
import com.foundaml.server.domain.repositories.DomainRepository
import com.foundaml.server.controllers.requests.PostProjectRequest
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
    domainService: DomainService,
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
      case FeaturesConfiguration(id, featureConfigurations) =>
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
     projectRequest: PostProjectRequest 
  ): IO[Project] = {
    val featuresIO = domainService.readFeatures(projectRequest.featuresId)
    val labelsIO = domainService.readLabels(projectRequest.labelsId)
    (featuresIO, labelsIO).mapN { (features, labels) => 
      projectRequest.problem match {
        case Classification =>
          ClassificationProject(
            projectRequest.id,
            projectRequest.name,
            ClassificationConfiguration(
              features,
              labels
            ),
            Nil,
            NoAlgorithm()
          )
        case Regression =>
          RegressionProject(
            projectRequest.id,
            projectRequest.name,
            RegressionConfiguration(
              features
            ),
            Nil,
            NoAlgorithm()
          )
      }
    }.flatMap { project => 
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
  }

  def readProject(id: String) =
    projectFactory.get(id).handleErrorWith {
      case UnexpectedEnd => IO.raiseError(ProjectDoesNotExist(id))
    }

  def updateProject(project: Project) = projectsRepository.update(project)
}
