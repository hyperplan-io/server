package com.foundaml.server.services

import org.scalatest._

import com.foundaml.server.services.domain._
import com.foundaml.server.models.features._
import com.foundaml.server.models.labels._
import com.foundaml.server.models.backends._
import com.foundaml.server.models._

class PredictionsServiceSpec extends FlatSpec {

  val predictionsService = new PredictionsService()

  it should "execute predictions correctly on local backend" in {
    val features = TensorFlowClassificationFeatures(
      "test instance",
      Nil
    )
    
    def compute(features: TensorFlowClassificationFeatures): TensorFlowClassificationLabels =
      TensorFlowClassificationLabels(
        List(
          TensorFlowClassicationLabel(
            List(1, 2, 3),
            List(0.0f, 0.1f, 0.2f),
            List("class1", "class2", "class3"),
            List(0.0f, 0.0f, 0.0f)
          )
        )
      ) 

    val defaultAlgorithm = Algorithm[TensorFlowClassificationFeatures, TensorFlowClassificationLabels](
      "algorithm id",
      Local(compute)
    )

    val project = Project(
      "id",
      "example project",
      Classification,
      Map.empty,
      DefaultAlgorithm(defaultAlgorithm) 
    )

    val prediction = predictionsService.predict(
      features,
      project,
      Some("algorithm id")
    )

    assert(prediction == compute(features))
  }
}
