package com.foundaml.server.test.domain.services

import com.foundaml.server.domain.{FoundaMLConfig, KinesisConfig}
import com.foundaml.server.domain.models.errors.FeaturesValidationFailed
import com.foundaml.server.domain.services.PredictionsService
import org.scalatest._
import org.scalatest.Inside.inside
import com.foundaml.server.domain.models.features._
import com.foundaml.server.domain.repositories.{PredictionsRepository, ProjectsRepository}
import com.foundaml.server.infrastructure.streaming.KinesisService
import com.foundaml.server.test.{ProjectGenerator, TestDatabase}
import org.http4s.client.blaze.Http1Client
import scalaz.zio.{DefaultRuntime, Task}
import scalaz.zio.interop.catz._

import scala.util.Try

class PredictionsServiceSpec
    extends FlatSpec
    with DefaultRuntime
    with TestDatabase {

  val config = FoundaMLConfig(
    KinesisConfig(enabled = false, "")
  )
  val kinesisService = unsafeRun(KinesisService("fake-region"))
  val projectRepository = new ProjectsRepository()(xa)
  val predictionsRepository = new PredictionsRepository()(xa)
  val predictionsService =
    new PredictionsService(projectRepository, predictionsRepository, kinesisService, config)

  it should "fail to execute predictions for an incorrect configuration" in {
    val features = CustomFeatures(
      List(
        StringFeature("test instance"),
        IntFeature(1),
        FloatFeature(0.5f)
      )
    )

    val project = ProjectGenerator.withLocalBackend()

    val shouldThrow = Try(
      unsafeRun(
        predictionsService
          .predict(
            features,
            project,
            Some("algorithm id")
          )
      )
    )
    inside(shouldThrow.toEither) {
      case Left(err) =>
        assert(
          err.getMessage
            .contains("The features are not correct for this project")
        )
    }

  }
}
