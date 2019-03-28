package com.foundaml.server.test.infrastructure

import java.util.UUID

import com.foundaml.server.domain.models.features.{
  FloatFeature,
  FloatVectorFeature
}
import com.foundaml.server.domain.models.labels.{ClassificationLabel, Labels}
import com.foundaml.server.domain.models.{
  ClassificationPrediction,
  Examples,
  Prediction
}
import com.foundaml.server.infrastructure.serialization.PredictionSerializer
import com.foundaml.server.test.SerializerTester
import io.circe.{Decoder, Encoder}
import org.scalatest.{FlatSpec, Matchers}

class PredictionSerializerSpec
    extends FlatSpec
    with SerializerTester
    with Matchers {

  val encoder: Encoder[ClassificationPrediction] =
    PredictionSerializer.classificationPredictionEncoder
  val decoder: Decoder[Prediction] = PredictionSerializer.decoder

  it should "correctly encode a prediction" in {

    val predictionId = UUID.randomUUID().toString
    val projectId = "test-project-encode"
    val algorithmId = "test-algorithm-encode"
    val labelId = UUID.randomUUID().toString
    val prediction = ClassificationPrediction(
      predictionId,
      projectId,
      algorithmId,
      List(
        FloatFeature(0.0f),
        FloatFeature(0.0f),
        FloatFeature(0.5f)
      ),
      Set.empty,
      Set(
        ClassificationLabel(
          "mylabel",
          0.5f,
          "correct_example_url",
          "incorrect_example_url"
        )
      )
    )

    testEncoder(prediction) { json =>
      val expectedJson =
        s"""{"type":"classification","id":"$predictionId","projectId":"$projectId","algorithmId":"$algorithmId","features":[0.0,0.0,0.5],"labels":[{"label":"mylabel","probability":0.5,"correctExampleUrl":"correct_example_url","incorrectExampleUrl":"incorrect_example_url"}],"examples":[]}"""
      json.noSpaces should be(expectedJson)
    }(encoder)
  }

  it should "correctly decode a prediction" in {

    val predictionId = UUID.randomUUID().toString
    val label = "mylabel"
    val projectId = "test-project-decode"
    val algorithmId = "test-algorithm-decode"
    val predictionJson =
      s"""{"type":"classification","id":"$predictionId","projectId":"$projectId","algorithmId":"$algorithmId","features":[0.0,0.0,0.5],"labels":[{"label":"mylabel","probability":0.5,"correctExampleUrl":"correct_example_url","incorrectExampleUrl":"incorrect_example_url"}],"examples":[]}"""

    testDecoder[Prediction](predictionJson) {
      case prediction: ClassificationPrediction =>
        prediction.id should be(predictionId)
        prediction.labels.head.label should be(label)
      case _ =>
        fail()
    }(decoder)
  }
}
