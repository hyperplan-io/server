package com.foundaml.server.test.infrastructure

import java.util.UUID

import com.foundaml.server.domain.models.Prediction
import com.foundaml.server.infrastructure.serialization.PredictionSerializer
import com.foundaml.server.test.SerializerTester
import io.circe.{Decoder, Encoder}
import org.scalatest.{FlatSpec, Matchers}

class PredictionSerializerSpec extends FlatSpec with SerializerTester with Matchers {

  val encoder: Encoder[Prediction] = PredictionSerializer.encoder
  val decoder: Decoder[Prediction] = PredictionSerializer.decoder

  it should "correctly encode a prediction" in {

    val predictionId = UUID.randomUUID().toString
    val prediction = Prediction(predictionId)

    testEncoder(prediction) { json =>
      val expectedJson = s"""{"id":"$predictionId"}"""
      json.noSpaces should be (expectedJson)
    }(encoder)
  }

  it should "correctly decode a prediction" in {

    val predictionId = UUID.randomUUID().toString
    val predictionJson = s"""{"id":"$predictionId"}"""
    testDecoder[Prediction](predictionJson) { prediction =>
      prediction.id should be (predictionId)
    }(decoder)
  }
}
