package com.hyperplan.server.test.domain.services

import org.scalatest._
import org.scalatest.Inside.inside
import com.hyperplan.domain.models.features._
import com.hyperplan.domain.models.features.transformers.TensorFlowFeaturesTransformer
import com.hyperplan.infrastructure.serialization.tensorflow.TensorFlowFeaturesSerializer
import io.circe._
import io.circe.syntax._
import io.circe.parser._

import scala.util.Random

class FeaturesTransformerServiceSpec extends FlatSpec {

  it should "transform features to a tensorflow classify compatible format" in {
    val f1 = Random.nextString(10)
    val f2 = Random.nextString(10)
    val f3 = Random.nextString(10)
    val features = List(
      StringFeature(f1, "test instance"),
      IntFeature(f2, 1),
      FloatFeature(f3, 0.5f)
    )

    val transformer = TensorFlowFeaturesTransformer(
      "my_signature_name",
      Map(
        f1 -> "test",
        f2 -> "toto",
        f3 -> "titi"
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
