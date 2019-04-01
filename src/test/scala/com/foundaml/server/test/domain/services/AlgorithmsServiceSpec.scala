package com.foundaml.server.test.domain.services

import com.foundaml.server.domain.factories.{AlgorithmFactory, ProjectFactory}
import com.foundaml.server.domain.repositories.{AlgorithmsRepository, ProjectsRepository}
import com.foundaml.server.domain.services.AlgorithmsService
import com.foundaml.server.test.{TaskChecker, TestDatabase}
import org.scalatest.FlatSpec
import org.scalatest.Inside.inside

class AlgorithmsServiceSpec extends FlatSpec with TestDatabase with TaskChecker {
  val algorithmsRepository = new AlgorithmsRepository()(xa)
  val projectsRepository = new ProjectsRepository()(xa)
  val algorithmFactory = new AlgorithmFactory(algorithmsRepository)
  val projectFactory = new ProjectFactory(projectsRepository, algorithmsRepository, algorithmFactory)

  val algorithmsService = new AlgorithmsService(algorithmsRepository, projectsRepository, projectFactory)

  it should "validate features configuration correctly" in {
    inside(algorithmsService.validateFeaturesConfiguration(1,2)) {
      case Some(err) =>
        assert(err.message == "The features dimension is incorrect for the project")
    }

    inside(algorithmsService.validateFeaturesConfiguration(1,1)) {
      case None =>
    }
  }

}
