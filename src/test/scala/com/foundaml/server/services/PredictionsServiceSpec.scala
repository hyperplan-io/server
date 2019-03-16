package com.foundaml.server.services

import com.foundaml.server.domain.services.PredictionsService
import org.scalatest._
import com.foundaml.server.domain.models.features._
import com.foundaml.server.utils._
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
        .map(prediction => assert(prediction == ProjectGenerator.computed))
    )
  }
}
