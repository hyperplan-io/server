package com.hyperplan.test.domain.services

import java.util.UUID

import com.hyperplan.domain.models.OneOfLabelsDescriptor
import com.hyperplan.domain.models.labels.{
  ClassificationLabel,
  TensorFlowClassificationLabels,
  TensorFlowClassificationLabel
}
import org.scalatest.Inside.inside
import org.scalatest._
import com.hyperplan.domain.models.labels.transformers.TensorFlowLabelsTransformer

class LabelsTransformerServiceSpec extends FlatSpec with Matchers {

  it should "not accept a label transformer with a different number of arguments than the labels" in {
    val labels1 = TensorFlowClassificationLabels(
      List(
        TensorFlowClassificationLabel(
          "tf_toto",
          0.5f
        )
      )
    )

    val transformer2 = TensorFlowLabelsTransformer(
      Map(
        "tf_toto" -> "toto",
        "tf_toto3" -> "toto3"
      )
    )

    import com.hyperplan.domain.models.LabelVectorDescriptor
    inside(
      transformer2.transform(
        LabelVectorDescriptor(
          "id",
          OneOfLabelsDescriptor(
            Set("toto", "toto3"),
            "Either toto or toto3"
          )
        ),
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
        TensorFlowClassificationLabel(
          "tf_toto",
          0.5f
        ),
        TensorFlowClassificationLabel(
          "tf_toto2",
          0.3f
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
        LabelVectorDescriptor(
          "id",
          OneOfLabelsDescriptor(
            Set("toto", "toto3"),
            "Either toto or toto3"
          )
        ),
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
        TensorFlowClassificationLabel(
          "tf_toto",
          0.5f
        ),
        TensorFlowClassificationLabel(
          "tf_titi",
          0.3f
        )
      )
    )

    val transformer = TensorFlowLabelsTransformer(
      Map(
        "tf_toto" -> "toto",
        "tf_titi" -> "titi"
      )
    )

    import com.hyperplan.domain.models.LabelVectorDescriptor
    val transformedLabels = transformer.transform(
      LabelVectorDescriptor(
        "id",
        OneOfLabelsDescriptor(
          Set("toto", "titi"),
          "Either toto or titi"
        )
      ),
      UUID.randomUUID().toString,
      labels
    )

    inside(transformedLabels) {
      case Right(tfLabels) =>
        inside(tfLabels.toList) {
          case ClassificationLabel("toto", 0.5f, _, _)
                :: ClassificationLabel("titi", 0.3f, _, _)
                :: Nil =>
        }
    }
  }

}
