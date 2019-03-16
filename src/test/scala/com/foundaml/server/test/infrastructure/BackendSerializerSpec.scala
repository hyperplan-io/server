package com.foundaml.server.test.infrastructure

import java.util.UUID

import com.foundaml.server.domain.models.Prediction
import com.foundaml.server.domain.models.backends.{Backend, TensorFlowBackend}
import com.foundaml.server.domain.models.features.transformers.TensorFlowFeaturesTransformer
import com.foundaml.server.domain.models.labels.transformers.TensorFlowLabelsTransformer
import com.foundaml.server.infrastructure.serialization.{BackendSerializer, PredictionSerializer}
import com.foundaml.server.test.SerializerTester
import io.circe.{Decoder, Encoder}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.Inside.inside

class BackendSerializerSpec extends FlatSpec with SerializerTester with Matchers {

  val encoder: Encoder[Backend] = BackendSerializer.encoder
  val decoder: Decoder[Backend] = BackendSerializer.decoder

  val doubleQuote = "\""

  it should "correctly encode a TensorFlow backend" in {

    val predictionId = UUID.randomUUID().toString

    val host = "127.0.0.1"
    val port = 8080
    val signatureName = "my signature"
    val featureFields = Set(
      "feature1",
      "feature2",
      "feature3",
      "feature4"
    )

    val labelFields = Set(
      "class1",
      "class2",
      "class3",
      "class4"
    )
    val featuresTransformer = TensorFlowFeaturesTransformer(
      signatureName,
      featureFields
    )

    val labelsTransformer = TensorFlowLabelsTransformer(
      labelFields
    )

    val backend = TensorFlowBackend(host, port, featuresTransformer, labelsTransformer)

    testEncoder(backend: Backend) { json =>
      val featuresJson = featureFields.map(field => s"$doubleQuote$field$doubleQuote").mkString(",")
      val labelsJson = labelFields.map(field => s"$doubleQuote$field$doubleQuote").mkString(",")

      val expectedJson =
        s"""{"host":"$host","port":$port,"featuresTransformer":{"signatureName":"$signatureName","fields":[$featuresJson]},"labelsTransformer":{"fields":[$labelsJson]},"class":"TensorFlowBackend"}"""
      json.noSpaces should be (expectedJson)
    }(encoder)
  }

  it should "correctly decode a TensorFlow backend" in {

    val expectedHost = "127.0.0.1"
    val expectedPort = 8080
    val expectedSignatureName = "my signature"
    val expectedFeatureFields = Set(
      "feature1",
      "feature2",
      "feature3",
      "feature4"
    )

    val expectedLabelFields = Set(
      "class1",
      "class2",
      "class3",
      "class4"
    )

    val expectedFeaturesTransformer = TensorFlowFeaturesTransformer(
      expectedSignatureName,
      expectedFeatureFields
    )

    val expectedLabelsTransformer = TensorFlowLabelsTransformer(
      expectedLabelFields
    )
    val featuresJson = expectedFeatureFields.map(field => s"$doubleQuote$field$doubleQuote").mkString(",")
    val labelsJson = expectedLabelFields.map(field => s"$doubleQuote$field$doubleQuote").mkString(",")

    val backendJson =
      s"""{"host":"$expectedHost","port":$expectedPort,"featuresTransformer":{"signatureName":"$expectedSignatureName","fields":[$featuresJson]},"labelsTransformer":{"fields":[$labelsJson]},"class":"TensorFlowBackend"}"""

    testDecoder[Backend](backendJson) { backend =>
      inside(backend) {
        case TensorFlowBackend(host, port, featuresTransformer, labelsTransformer) =>
          host should be (expectedHost)
          port should be (expectedPort)
          featuresTransformer should be (expectedFeaturesTransformer)
          labelsTransformer should be (expectedLabelsTransformer)
      }
    }(decoder)
  }

}
