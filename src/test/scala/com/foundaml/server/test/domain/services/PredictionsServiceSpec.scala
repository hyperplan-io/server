package com.foundaml.server.test.domain.services

import com.foundaml.server.domain.services.PredictionsService
import org.scalatest._
import org.scalatest.Inside.inside
import com.foundaml.server.domain.models.features._
import com.foundaml.server.test.ProjectGenerator
import scalaz.zio.DefaultRuntime

class PredictionsServiceSpec extends FlatSpec with DefaultRuntime {

  val predictionsService = new PredictionsService()

  it should "execute predictions correctly on local backend" in {
    val features = Features(
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
            case Left(
                com.foundaml.server.domain.models.errors
                  .NoAlgorithmAvailable(message)
                ) =>
              assert(message == "No algorithms are setup")
          }
        }
    )
  }
}
