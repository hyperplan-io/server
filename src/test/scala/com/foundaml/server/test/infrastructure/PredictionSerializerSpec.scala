package com.foundaml.server.test.infrastructure

import java.util.UUID

import com.foundaml.server.domain.models.features.DoubleFeatures
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
    val prediction = Prediction(
      predictionId,
      projectId,
      algorithmId,
      DoubleFeatures(
        List(
          0.0,
          0.1,
          0.5
        )
      ),
      Labels(
        Set(
          ClassificationLabel(
            "",
            0.5f
          )
        )
      ),
      Examples(None)
    )

    testEncoder(prediction) { json =>
      val expectedJson =
        s"""{"id":"$predictionId","projectId":"$projectId","algorithmId":"$algorithmId","features":{"data":[0.0,0.1,0.5],"class":"DoubleFeatures"},"labels":{"labels":[{"label":"","probability":0.5,"class":"ClassificationLabel"}]},"examples":{"examples":null}}"""
      json.noSpaces should be(expectedJson)
    }(encoder)
  }

  it should "correctly decode a prediction" in {

    val predictionId = UUID.randomUUID().toString
    val projectId = "test-project-decode"
    val algorithmId = "test-algorithm-decode"
    val predictionJson =
      s"""{"id":"$predictionId","projectId":"$projectId","algorithmId":"$algorithmId","features":{"data":[0.0,0.1,0.5],"class":"DoubleFeatures"},"labels":{"labels":[{"label":"","probability":0.5,"class":"ClassificationLabel"}]},"examples":{"examples":null}}"""
    testDecoder[Prediction](predictionJson) { prediction =>
      prediction.id should be(predictionId)
    }(decoder)
  }
}
