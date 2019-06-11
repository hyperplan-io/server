package com.hyperplan.server.test.infrastructure

import java.util.UUID

import com.hyperplan.domain.models.features.FloatFeature
import com.hyperplan.domain.models.labels.ClassificationLabel
import com.hyperplan.domain.models.{ClassificationPrediction, Prediction}
import com.hyperplan.infrastructure.serialization.PredictionSerializer
import com.hyperplan.server.test.SerializerTester
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
        FloatFeature("f1", 0.0f),
        FloatFeature("f2", 0.0f),
        FloatFeature("f3", 0.5f)
      ),
      List.empty,
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
        s"""{"type":"classification","id":"$predictionId","projectId":"$projectId","algorithmId":"$algorithmId","features":[{"key":"f1","type":"Float","dimension":"One","value":0.0},{"key":"f2","type":"Float","dimension":"One","value":0.0},{"key":"f3","type":"Float","dimension":"One","value":0.5}],"labels":[{"label":"mylabel","probability":0.5,"correctExampleUrl":"correct_example_url","incorrectExampleUrl":"incorrect_example_url"}],"examples":[]}"""
      json.noSpaces should be(expectedJson)
    }(encoder)
  }

  it should "correctly decode a prediction" in {

    val predictionId = UUID.randomUUID().toString
    val label = "mylabel"
    val projectId = "test-project-decode"
    val algorithmId = "test-algorithm-decode"
    val predictionJson =
      s"""{"type":"classification","id":"$predictionId","projectId":"$projectId","algorithmId":"$algorithmId","features":[{"key":"f1","type":"Float","dimension":"One","value":0.0},{"key":"f2","type":"Float","dimension":"One","value":0.0},{"key":"f3","type":"Float","dimension":"One","value":0.5}],"labels":[{"label":"mylabel","probability":0.5,"correctExampleUrl":"correct_example_url","incorrectExampleUrl":"incorrect_example_url"}],"examples":[]}"""

    testDecoder[Prediction](predictionJson) {
      case prediction: ClassificationPrediction =>
        prediction.id should be(predictionId)
        prediction.labels.head.label should be(label)
      case _ =>
        fail()
    }(decoder)
  }
}
