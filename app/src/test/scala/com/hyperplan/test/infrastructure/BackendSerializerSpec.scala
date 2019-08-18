package com.hyperplan.test.infrastructure

import cats.implicits._
import java.util.UUID

import com.hyperplan.domain.models.backends.{
  Backend,
  TensorFlowClassificationBackend,
  TensorFlowRegressionBackend
}
import com.hyperplan.domain.models.features.transformers.TensorFlowFeaturesTransformer
import com.hyperplan.domain.models.labels.transformers.TensorFlowLabelsTransformer
import com.hyperplan.infrastructure.serialization.BackendSerializer
import com.hyperplan.test.SerializerTester
import com.hyperplan.test.SerializerTester
import io.circe.{Decoder, Encoder, Json}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.Inside.inside
import com.hyperplan.domain.models.backends.BasicHttpClassification

class BackendSerializerSpec
    extends FlatSpec
    with SerializerTester
    with Matchers {

  val encoder: Encoder[Backend] = BackendSerializer.encoder
  val decoder: Decoder[Backend] = BackendSerializer.decoder

  val doubleQuote = "\""

  it should "correctly encode a TensorFlow classification backend" in {

    val predictionId = UUID.randomUUID().toString

    val rootPath = "http://127.0.0.1:8080"
    val model = "myModel"
    val modelVersion = "v0.1".some
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
        rootPath,
        model,
        modelVersion,
        featuresTransformer,
        labelsTransformer
      )

    testEncoder(backend: Backend) { json =>
      val featuresJson =
        """{"keyf1":"feature1","keyf2":"feature2","keyf3":"feature3","keyf4":"feature4"}"""

      val labelsJson =
        """{"tf_class1":"class1","tf_class2":"class2","tf_class3":"class3","tf_class4":"class4"}"""
      val expectedJson =
        s"""{"class":"TensorFlowClassificationBackend","rootPath":"$rootPath","model":"$model","modelVersion":"${modelVersion
          .getOrElse(
            Json.Null
          )}","featuresTransformer":{"signatureName":"$signatureName","mapping":$featuresJson},"labelsTransformer":{"fields":$labelsJson}}"""
      json.noSpaces should be(expectedJson)
    }(encoder)
  }

  it should "correctly decode a TensorFlow classification backend" in {

    val expectedRootPath = "http://127.0.0.1:8080"
    val expectedModel = "myModel"
    val expectedModelVersion = "v0.1".some
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
      s"""{"class":"TensorFlowClassificationBackend","rootPath":"$expectedRootPath","model":"$expectedModel","modelVersion":"${expectedModelVersion
        .getOrElse(Json.Null)}","featuresTransformer":{"signatureName":"$expectedSignatureName","mapping":$featuresJson},"labelsTransformer":{"fields":$labelsJson}}"""

    testDecoder[Backend](backendJson) { backend =>
      inside(backend) {
        case TensorFlowClassificationBackend(
            rootPath,
            model,
            modelVersion,
            featuresTransformer,
            labelsTransformer
            ) =>
          rootPath should be(expectedRootPath)
          model should be(expectedModel)
          modelVersion should be(expectedModelVersion)
          featuresTransformer should be(expectedFeaturesTransformer)
          labelsTransformer should be(expectedLabelsTransformer)
      }
    }(decoder)
  }

  it should "correctly encode a TensorFlow regression backend" in {

    val predictionId = UUID.randomUUID().toString

    val rootPath = "http://127.0.0.1:8080"
    val model = "myModel"
    val modelVersion = "v0.1".some
    val signatureName = "my signature"
    val featureFields = Map(
      "keyf1" -> "feature1",
      "keyf2" -> "feature2",
      "keyf3" -> "feature3",
      "keyf4" -> "feature4"
    )

    val featuresTransformer = TensorFlowFeaturesTransformer(
      signatureName,
      featureFields
    )

    val backend =
      TensorFlowRegressionBackend(
        rootPath,
        model,
        modelVersion,
        featuresTransformer
      )

    testEncoder(backend: Backend) { json =>
      val featuresJson =
        """{"keyf1":"feature1","keyf2":"feature2","keyf3":"feature3","keyf4":"feature4"}"""

      val labelsJson =
        """{"tf_class1":"class1","tf_class2":"class2","tf_class3":"class3","tf_class4":"class4"}"""
      val expectedJson =
        s"""{"class":"TensorFlowRegressionBackend","rootPath":"$rootPath","model":"$model","modelVersion":"${modelVersion
          .getOrElse(
            Json.Null
          )}","featuresTransformer":{"signatureName":"$signatureName","mapping":$featuresJson}}"""
      json.noSpaces should be(expectedJson)
    }(encoder)
  }

  it should "correctly decode a TensorFlow regression backend" in {

    val expectedRootPath = "http://127.0.0.1:8080"
    val expectedModel = "myModel"
    val expectedModelVersion = "v0.1".some
    val expectedSignatureName = "my signature"
    val expectedFeatureFields = Map(
      "keyf1" -> "feature1",
      "keyf2" -> "feature2",
      "keyf3" -> "feature3",
      "keyf4" -> "feature4"
    )

    val expectedFeaturesTransformer = TensorFlowFeaturesTransformer(
      expectedSignatureName,
      expectedFeatureFields
    )

    val featuresJson =
      """{"keyf1":"feature1","keyf2":"feature2","keyf3":"feature3","keyf4":"feature4"}"""

    val backendJson =
      s"""{"class":"TensorFlowRegressionBackend","rootPath":"$expectedRootPath","model":"$expectedModel","modelVersion":"${expectedModelVersion
        .getOrElse(Json.Null)}","featuresTransformer":{"signatureName":"$expectedSignatureName","mapping":$featuresJson}}"""

    testDecoder[Backend](backendJson) { backend =>
      inside(backend) {
        case TensorFlowRegressionBackend(
            rootPath,
            model,
            modelVersion,
            featuresTransformer
            ) =>
          rootPath should be(expectedRootPath)
          model should be(expectedModel)
          modelVersion should be(expectedModelVersion)
          featuresTransformer should be(expectedFeaturesTransformer)
      }
    }(decoder)
  }

  it should "correctly encode a basic http backend" in {

    val predictionId = UUID.randomUUID().toString

    val rootPath = "http://127.0.0.1:8080"

    val backend =
      BasicHttpClassification(
        rootPath
      )

    testEncoder(backend: Backend) { json =>
      val expectedJson =
        s"""{"class":"BasicHttpClassification","rootPath":"$rootPath"}"""
      json.noSpaces should be(expectedJson)
    }(encoder)
  }

  it should "correctly decode a basic http classification backend" in {

    val expectedRootPath = "http://127.0.0.1:8080"
    val backendJson =
      s"""{"class":"BasicHttpClassification","rootPath":"$expectedRootPath"}"""

    testDecoder[Backend](backendJson) { backend =>
      inside(backend) {
        case BasicHttpClassification(
            rootPath,
            _
            ) =>
          rootPath should be(expectedRootPath)
      }
    }(decoder)
  }
}
