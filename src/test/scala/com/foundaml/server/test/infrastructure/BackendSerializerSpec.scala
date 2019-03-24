package com.foundaml.server.test.infrastructure

import java.util.UUID

import com.foundaml.server.domain.models.Prediction
import com.foundaml.server.domain.models.backends.{Backend, TensorFlowClassificationBackend}
import com.foundaml.server.domain.models.features.transformers.TensorFlowFeaturesTransformer
import com.foundaml.server.domain.models.labels.transformers.TensorFlowLabelsTransformer
import com.foundaml.server.infrastructure.serialization.{
  BackendSerializer,
  PredictionSerializer
}
import com.foundaml.server.test.SerializerTester
import io.circe.{Decoder, Encoder}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.Inside.inside

class BackendSerializerSpec
    extends FlatSpec
    with SerializerTester
    with Matchers {

  val encoder: Encoder[Backend] = BackendSerializer.Implicits.encoder
  val decoder: Decoder[Backend] = BackendSerializer.Implicits.decoder

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

    val labelFields = Map(
      "tf_class1" -> "class1",
      "tf_class2" -> "class2",
      "tf_class3" -> "class3",
      "tf_class4" -> "class4"
    )
    val featuresTransformer = TensorFlowFeaturesTransformer(
      signatureName,
      featureFields
    )

    val labelsTransformer = TensorFlowLabelsTransformer(
      labelFields
    )

    val backend =
      TensorFlowClassificationBackend(host, port, featuresTransformer, labelsTransformer)

    testEncoder(backend: Backend) { json =>
      val featuresJson = featureFields
        .map(field => s"$doubleQuote$field$doubleQuote")
        .mkString(",")

      val labelsJson =
        """{"tf_class1":"class1","tf_class2":"class2","tf_class3":"class3","tf_class4":"class4"}"""

      val expectedJson =
        s"""{"host":"$host","port":$port,"featuresTransformer":{"signatureName":"$signatureName","fields":[$featuresJson]},"labelsTransformer":{"fields":$labelsJson},"class":"TensorFlowBackend"}"""
      json.noSpaces should be(expectedJson)
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

    val expectedLabelFields = Map(
      "tf_class1" -> "class1",
      "tf_class2" -> "class2",
      "tf_class3" -> "class3",
      "tf_class4" -> "class4"
    )

    val expectedFeaturesTransformer = TensorFlowFeaturesTransformer(
      expectedSignatureName,
      expectedFeatureFields
    )

    val expectedLabelsTransformer = TensorFlowLabelsTransformer(
      expectedLabelFields
    )
    val featuresJson = expectedFeatureFields
      .map(field => s"$doubleQuote$field$doubleQuote")
      .mkString(",")
    val labelsJson =
      """{"tf_class1":"class1","tf_class2":"class2","tf_class3":"class3","tf_class4":"class4"}"""

    val backendJson =
      s"""{"host":"$expectedHost","port":$expectedPort,"featuresTransformer":{"signatureName":"$expectedSignatureName","fields":[$featuresJson]},"labelsTransformer":{"fields":$labelsJson},"class":"TensorFlowBackend"}"""

    testDecoder[Backend](backendJson) { backend =>
      inside(backend) {
        case TensorFlowClassificationBackend(
            host,
            port,
            featuresTransformer,
            labelsTransformer
            ) =>
          host should be(expectedHost)
          port should be(expectedPort)
          featuresTransformer should be(expectedFeaturesTransformer)
          labelsTransformer should be(expectedLabelsTransformer)
      }
    }(decoder)
  }

}
