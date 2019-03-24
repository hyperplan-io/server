package com.foundaml.server.test.domain.services

import java.util.UUID

import com.foundaml.server.domain.factories.ProjectFactory
import com.foundaml.server.domain.{
  DatabaseConfig,
  FoundaMLConfig,
  KinesisConfig,
  PostgreSqlConfig
}
import com.foundaml.server.domain.models.errors.FeaturesValidationFailed
import com.foundaml.server.domain.services.PredictionsService
import org.scalatest._
import org.scalatest.Inside.inside
import com.foundaml.server.domain.models.features._
import com.foundaml.server.domain.repositories.{
  AlgorithmsRepository,
  PredictionsRepository,
  ProjectsRepository
}
import com.foundaml.server.infrastructure.streaming.KinesisService
import com.foundaml.server.test.{
  AlgorithmGenerator,
  ProjectGenerator,
  TestDatabase
}
import org.http4s.client.blaze.Http1Client
import scalaz.zio.{DefaultRuntime, Task}
import scalaz.zio.interop.catz._

import scala.util.Try

class PredictionsServiceSpec
    extends FlatSpec
    with DefaultRuntime
    with TestDatabase {

  val config = FoundaMLConfig(
    KinesisConfig(enabled = false, "predictionsStream", "examplesStream"),
    DatabaseConfig(
      PostgreSqlConfig(
        "host",
        5432,
        "database",
        "username",
        "password"
      )
    )
  )
  val kinesisService = unsafeRun(KinesisService("fake-region"))
  val projectsRepository = new ProjectsRepository()(xa)
  val algorithmsRepository = new AlgorithmsRepository()(xa)
  val predictionsRepository = new PredictionsRepository()(xa)
  val projectFactory =
    new ProjectFactory(projectsRepository, algorithmsRepository)
  val predictionsService =
    new PredictionsService(
      projectsRepository,
      predictionsRepository,
      kinesisService,
      projectFactory,
      config
    )

  it should "fail to execute predictions for an incorrect configuration" in {
    val features = List(
      StringFeature("test instance"),
      IntFeature(1),
      FloatFeature(0.5f)
    )

    val algorithmId = UUID.randomUUID().toString
    val project = ProjectGenerator.withLocalBackend(
      Some(List(AlgorithmGenerator.withLocalBackend(Some(algorithmId))))
    )

    val shouldThrow = Try(
      unsafeRun(
        predictionsService
          .predictForProject(
            project,
            features,
            Some(algorithmId)
          )
      )
    )
    inside(shouldThrow.toEither) {
      case Left(err) =>
        assert(
          err.getMessage
            .contains("The features do not match the configuration of project")
        )
    }

  }
}
