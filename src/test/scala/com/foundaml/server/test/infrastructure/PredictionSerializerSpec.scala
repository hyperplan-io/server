package com.foundaml.server.test.infrastructure

import java.util.UUID

import com.foundaml.server.domain.models.features.{FloatFeature, FloatFeatures}
import com.foundaml.server.domain.models.labels.{ClassificationLabel, Labels}
import com.foundaml.server.domain.models.{Examples, Prediction}
import com.foundaml.server.infrastructure.serialization.PredictionSerializer
import com.foundaml.server.test.SerializerTester
import io.circe.{Decoder, Encoder}
import org.scalatest.{FlatSpec, Matchers}

class PredictionSerializerSpec
    extends FlatSpec
    with SerializerTester
    with Matchers {

  val encoder: Encoder[Prediction] = PredictionSerializer.encoder
  val decoder: Decoder[Prediction] = PredictionSerializer.decoder

  it should "correctly encode a prediction" in {

    val predictionId = UUID.randomUUID().toString
    val projectId = "test-project-encode"
    val algorithmId = "test-algorithm-encode"
    val labelId = UUID.randomUUID().toString
    val prediction = Prediction(
      predictionId,
      projectId,
      algorithmId,
      List(
        FloatFeature(0.0f),
        FloatFeature(0.0f),
        FloatFeature(0.5f)
      ),
      Labels(
        Set(
          ClassificationLabel(
            labelId,
            "",
            0.5f,
            "correct_example_url",
            "incorrect_example_url"
          )
        )
      ),
      Set.empty
    )

    testEncoder(prediction) { json =>
      println(json.noSpaces)
      val expectedJson =
        s"""{"id":"$predictionId","projectId":"$projectId","algorithmId":"$algorithmId","features":[0.0,0.0,0.5],"labels":{"labels":[{"id":"$labelId","label":"","probability":0.5,"correctExampleUrl":"correct_example_url","incorrectExampleUrl":"incorrect_example_url","class":"ClassificationLabel"}]},"examples":[]}"""
      json.noSpaces should be(expectedJson)
    }(encoder)
  }

  it should "correctly decode a prediction" in {

    val predictionId = UUID.randomUUID().toString
    val labelId = UUID.randomUUID().toString
    val projectId = "test-project-decode"
    val algorithmId = "test-algorithm-decode"
    val predictionJson =
      s"""{"id":"$predictionId","projectId":"$projectId","algorithmId":"$algorithmId","features":[0.0,0.2,0.5],"labels":{"labels":[{"id":"$labelId","label":"","probability":0.5,"correctExampleUrl":"correct_example_url","incorrectExampleUrl":"incorrect_example_url","class":"ClassificationLabel"}]},"examples":[]}"""
    testDecoder[Prediction](predictionJson) { prediction =>
      prediction.id should be(predictionId)
      prediction.labels.labels.head.id should be(labelId)
    }(decoder)
  }
}
