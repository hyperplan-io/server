package com.hyperplan.test.infrastructure

import java.util.UUID

import com.hyperplan.domain.models.backends.{
  Backend,
  TensorFlowClassificationBackend
}
import com.hyperplan.domain.models.features.transformers.TensorFlowFeaturesTransformer
import com.hyperplan.domain.models.labels.transformers.TensorFlowLabelsTransformer
import com.hyperplan.infrastructure.serialization.BackendSerializer
import com.hyperplan.test.SerializerTester
import com.hyperplan.test.SerializerTester
import io.circe.{Decoder, Encoder}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.Inside.inside

class BackendSerializerSpec
    extends FlatSpec
    with SerializerTester
    with Matchers {

  val encoder: Encoder[Backend] = BackendSerializer.encoder
  val decoder: Decoder[Backend] = BackendSerializer.decoder

  val doubleQuote = "\""

  it should "correctly encode a TensorFlow backend" in {

    val predictionId = UUID.randomUUID().toString

    val host = "127.0.0.1"
    val port = 8080
    val signatureName = "my signature"
    val featureFields = Map(
      "keyf1" -> "feature1",
      "keyf2" -> "feature2",
      "keyf3" -> "feature3",
      "keyf4" -> "feature4"
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
      TensorFlowClassificationBackend(
        host,
        port,
        featuresTransformer,
        labelsTransformer
      )

    testEncoder(backend: Backend) { json =>
      val featuresJson =
        """{"keyf1":"feature1","keyf2":"feature2","keyf3":"feature3","keyf4":"feature4"}"""

      val labelsJson =
        """{"tf_class1":"class1","tf_class2":"class2","tf_class3":"class3","tf_class4":"class4"}"""
      val expectedJson =
        s"""{"class":"TensorFlowClassificationBackend","host":"$host","port":$port,"featuresTransformer":{"signatureName":"$signatureName","mapping":$featuresJson},"labelsTransformer":{"fields":$labelsJson}}"""
      json.noSpaces should be(expectedJson)
    }(encoder)
  }

  it should "correctly decode a TensorFlow backend" in {

    val expectedHost = "127.0.0.1"
    val expectedPort = 8080
    val expectedSignatureName = "my signature"
    val expectedFeatureFields = Map(
      "keyf1" -> "feature1",
      "keyf2" -> "feature2",
      "keyf3" -> "feature3",
      "keyf4" -> "feature4"
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
    val featuresJson =
      """{"keyf1":"feature1","keyf2":"feature2","keyf3":"feature3","keyf4":"feature4"}"""
    val labelsJson =
      """{"tf_class1":"class1","tf_class2":"class2","tf_class3":"class3","tf_class4":"class4"}"""

    val backendJson =
      s"""{"class":"TensorFlowClassificationBackend","host":"$expectedHost","port":$expectedPort,"featuresTransformer":{"signatureName":"$expectedSignatureName","mapping":$featuresJson},"labelsTransformer":{"fields":$labelsJson}}"""

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
