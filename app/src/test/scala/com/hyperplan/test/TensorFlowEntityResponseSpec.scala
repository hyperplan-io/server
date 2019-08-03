package com.hyperplan.test

import com.hyperplan.domain.models.labels.{
  TensorFlowClassificationLabel,
  TensorFlowClassificationLabels
}
import com.hyperplan.infrastructure.serialization.tensorflow.TensorFlowClassificationLabelsSerializer
import io.circe._
import io.circe.parser._
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.Inside._

class TensorFlowEntityResponseSpec extends FlatSpec with Matchers {

  implicit val encoder: Encoder[TensorFlowClassificationLabels] =
    TensorFlowClassificationLabelsSerializer.encoder
  implicit val decoder: Decoder[TensorFlowClassificationLabels] =
    TensorFlowClassificationLabelsSerializer.decoder

  it should "correctly encode and decode TensorFlowEntityResponse" in {
    val rawJson =
      """
        |{
        |"result" : [
        |    [
        |      "feature-1",
        |      0.5
        |    ]
        |  ]
        |}
    """.stripMargin

    inside(parse(rawJson)) {
      case Right(json) =>
        json.as[TensorFlowClassificationLabels]
      case Left(err) =>
        println(err)
        fail()
    }
  }

  it should "correctly encode and decode TensorFlowClassificationLabel" in {
    val rawJson =
      """
        |{
        |"result" : [
        |    [
        |      "feature-1",
        |      0.5
        |    ]
        |  ]
        |}
    """.stripMargin

    inside(parse(rawJson)) {
      case Right(json) =>
        json.as[TensorFlowClassificationLabels](
          TensorFlowClassificationLabelsSerializer.decoder
        )
    }
  }
}
