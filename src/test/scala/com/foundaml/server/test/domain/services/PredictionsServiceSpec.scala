package com.foundaml.server.test.domain.services

import com.foundaml.server.domain.models.errors.FeaturesValidationFailed
import com.foundaml.server.domain.services.PredictionsService
import org.scalatest._
import org.scalatest.Inside.inside
import com.foundaml.server.domain.models.features._
import com.foundaml.server.domain.repositories.ProjectsRepository
import com.foundaml.server.test.{ProjectGenerator, TestDatabase}
import scalaz.zio.DefaultRuntime

class PredictionsServiceSpec extends FlatSpec with DefaultRuntime with TestDatabase {

  val projectRepository = new ProjectsRepository()(xa)
  val predictionsService = new PredictionsService(projectRepository)

  it should "fail to execute predictions for an incorrect configuration" in {
    val features = CustomFeatures(
      List(
        StringFeature("test instance"),
        IntFeature(1),
        FloatFeature(0.5f)
      )
    )

    val project = ProjectGenerator.withLocalBackend()

    unsafeRun(
      predictionsService
        .predict(
          features,
          project,
          Some("algorithm id")
        )
        .map { prediction =>
          inside(prediction) {
            case Left(FeaturesValidationFailed(message)) =>
              assert(message == "The features are not correct for this project")
          }
        }
    )
  }
}
