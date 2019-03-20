package com.foundaml.server.test.domain.services

import org.scalatest._
import org.scalatest.Inside.inside
import com.foundaml.server.domain.models.features._
import com.foundaml.server.domain.models.features.transformers.TensorFlowFeaturesTransformer
import com.foundaml.server.infrastructure.serialization.{
  FeaturesSerializer,
  TensorFlowFeaturesSerializer
}
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import scalaz.zio.DefaultRuntime

class FeaturesTransformerServiceSpec extends FlatSpec with DefaultRuntime {

  it should "not accept a feature transformer with a different number of arguments than the features" in {

    val features3 = CustomFeatures(
      List(
        StringFeature("test instance"),
        IntFeature(1),
        DoubleFeature(0.5f)
      )
    )

    val transformer2 = TensorFlowFeaturesTransformer(
      "my_signature_name",
      Set(
        "test",
        "toto"
      )
    )

    inside(
      transformer2.transform(
        features3
      )
    ) {
      case Left(err) =>
    }

    val features2 = CustomFeatures(
      List(
        StringFeature("test instance"),
        IntFeature(1)
      )
    )
    val transformer3 = TensorFlowFeaturesTransformer(
      "my_signature_name",
      Set(
        "test",
        "toto",
        "titi"
      )
    )

    inside(
      transformer3.transform(
        features2
      )
    ) {
      case Left(err) =>
    }
  }

  it should "transform features to a tensorflow classify compatible format" in {
    val features = CustomFeatures(
      List(
        StringFeature("test instance"),
        IntFeature(1),
        DoubleFeature(0.5f)
      )
    )

    val transformer = TensorFlowFeaturesTransformer(
      "my_signature_name",
      Set(
        "test",
        "toto",
        "titi"
      )
    )

    val transformedFeatures = transformer.transform(
      features
    )
    val expectedJson = parse(
      """
        {
          "signature_name": "my_signature_name",
          "examples": [
            { 
              "test": "test instance",
              "toto": 1,
              "titi": 0.5
            }
          ]
        }
        """
    ).getOrElse(Json.Null)

    inside(transformedFeatures) {
      case Right(tfFeatures) =>
        assert(
          Json.eqJson
            .eqv(
              TensorFlowFeaturesSerializer.encodeJson(tfFeatures),
              expectedJson
            )
        )
    }
  }
}
