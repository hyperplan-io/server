package com.foundaml.server.services

import org.scalatest._
import org.scalatest.Inside.inside

import com.foundaml.server.domain.models.features._
import com.foundaml.server.domain.models.features.transformers._
import com.foundaml.server.domain.models.labels._
import com.foundaml.server.domain.models.backends._
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.features.transformers.TensorFlowFeaturesTransformer
import com.foundaml.server.utils._

import com.foundaml.server.infrastructure.serialization.FeaturesSerializer

import io.circe._
import io.circe.syntax._
import io.circe.parser._

import scalaz.zio.DefaultRuntime

class FeaturesTransformerServiceSpec extends FlatSpec with DefaultRuntime {

  it should "not accept a feature transformer with a different number of arguments than the features" in {
    val features3 = Features(
      List(
        StringFeature("test instance"),
        IntFeature(1),
        FloatFeature(0.5f)
      )
    )

    val transformer2 = new TensorFlowFeaturesTransformer(
      "my_signature_name",
      List(
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

    val features2 = Features(
      List(
        StringFeature("test instance"),
        IntFeature(1)
      )
    )
    val transformer3 = new TensorFlowFeaturesTransformer(
      "my_signature_name",
      List(
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
    val features = Features(
      List(
        StringFeature("test instance"),
        IntFeature(1),
        FloatFeature(0.5f)
      )
    )

    val transformer = new TensorFlowFeaturesTransformer(
      "my_signature_name",
      List(
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
            .eqv(FeaturesSerializer.encodeJson(tfFeatures), expectedJson)
        )
    }
  }
}
