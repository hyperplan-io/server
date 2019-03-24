package com.foundaml.server.test.domain.services

import java.util.UUID

import com.foundaml.server.domain.models.labels.ClassificationLabel
import scalaz.zio.DefaultRuntime
import org.scalatest.Inside.inside
import org.scalatest._
import com.foundaml.server.domain.models.labels.transformers.{
  TensorFlowLabel,
  TensorFlowClassificationLabels,
  TensorFlowLabelsTransformer
}

class LabelsTransformerServiceSpec
    extends FlatSpec
    with DefaultRuntime
    with Matchers {

  it should "not accept a label transformer with a different number of arguments than the labels" in {
    val labels1 = TensorFlowClassificationLabels(
      List(
        List(
          "tf_toto" -> 0.5f
        )
      )
    )

    val transformer2 = TensorFlowLabelsTransformer(
      Map(
        "tf_toto" -> "toto",
        "tf_toto3" -> "toto3"
      )
    )

    inside(
      transformer2.transform(
        UUID.randomUUID().toString,
        labels1
      )
    ) {
      case Left(err) =>
        assert(
          err.message == "labels do not exactly match with classification results"
        )
    }

    val labels2 = TensorFlowClassificationLabels(
      List(
        List(
          "tf_toto" -> 0.5f
        ),
        List(
          "tf_toto2" -> 0.3f
        )
      )
    )

    val transformer1 = TensorFlowLabelsTransformer(
      Map(
        "tf_toto" -> "toto"
      )
    )

    inside(
      transformer1.transform(
        UUID.randomUUID().toString,
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

    val labels = TensorFlowClassificationLabels(
      List(
        List(
          "tf_toto" -> 0.5f
        ),
        List(
          "tf_titi" -> 0.3f
        )
      )
    )

    val transformer = TensorFlowLabelsTransformer(
      Map(
        "tf_toto" -> "toto",
        "tf_titi" -> "titi"
      )
    )

    val transformedFeatures = transformer.transform(
      UUID.randomUUID().toString,
      labels
    )

    inside(transformedFeatures) {
      case Right(tfLabels) =>
        inside(tfLabels.labels.toList) {
          case ClassificationLabel(_, "toto", 0.5f, _, _)
                :: ClassificationLabel(_, "titi", 0.3f, _, _)
                :: Nil =>
        }
    }
  }

}
