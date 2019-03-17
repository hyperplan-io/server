package com.foundaml.server.test.domain.services

import com.foundaml.server.domain.models.labels.ClassificationLabel
import scalaz.zio.DefaultRuntime
import org.scalatest.Inside.inside
import org.scalatest._
import com.foundaml.server.domain.models.labels.transformers.{
  TensorFlowLabel,
  TensorFlowLabels,
  TensorFlowLabelsTransformer
}

class LabelsTransformerServiceSpec
    extends FlatSpec
    with DefaultRuntime
    with Matchers {

  it should "not accept a label transformer with a different number of arguments than the labels" in {
    val labels1 = TensorFlowLabels(
      List(
        List(
          "toto" -> 0.5f
        )
      )
    )

    val transformer2 = new TensorFlowLabelsTransformer(
      Set(
        "toto",
        "toto3"
      )
    )

    inside(
      transformer2.transform(
        labels1
      )
    ) {
      case Left(err) =>
        assert(
          err.message == "labels do not exactly match with classification results"
        )
    }

    val labels2 = TensorFlowLabels(
      List(
        List(
          "toto" -> 0.5f
        ),
        List(
          "toto2"-> 0.3f
        )
      )
    )

    val transformer1 = new TensorFlowLabelsTransformer(
      Set(
        "toto"
      )
    )

    inside(
      transformer1.transform(
        labels2
      )
    ) {
      case Left(err) =>
        assert(
          err.message == "labels do not exactly match with classification results"
        )
    }
  }

  it should "transform labels to a foundaml compatible format" in {

    val labels = TensorFlowLabels(
      List(
        List(
          "toto" -> 0.5f
        ),
        List(
          "titi"-> 0.3f
        )
      )
    )

    val transformer = new TensorFlowLabelsTransformer(
      Set(
        "toto",
        "titi"
      )
    )

    val transformedFeatures = transformer.transform(
      labels
    )

    inside(transformedFeatures) {
      case Right(tfLabels) =>
        val expected = Set(
          ClassificationLabel(
            "toto",
            0.5f
          ),
          ClassificationLabel(
            "titi",
            0.3f
          )
        )
        tfLabels.labels should be(expected)
    }
  }

}
