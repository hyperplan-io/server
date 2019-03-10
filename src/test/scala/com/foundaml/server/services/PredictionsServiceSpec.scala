package com.foundaml.server.services

import org.scalatest._

import com.foundaml.server.services.domain._
import com.foundaml.server.models.features._
import com.foundaml.server.models.labels._
import com.foundaml.server.models.backends._
import com.foundaml.server.models._

import com.foundaml.server.utils._

import scalaz.zio.DefaultRuntime

class PredictionsServiceSpec extends FlatSpec with DefaultRuntime {

  val predictionsService = new PredictionsService()

  it should "execute predictions correctly on local backend" in {
    val features = TensorFlowClassificationFeatures(
      "test instance",
      Nil
    )

    val project = ProjectGenerator.withLocalBackend()
    unsafeRun(predictionsService.predict(
      features,
      project,
      Some("algorithm id")
      ).map(prediction => 
        assert(prediction == ProjectGenerator.computed)
      ))
  }
}
