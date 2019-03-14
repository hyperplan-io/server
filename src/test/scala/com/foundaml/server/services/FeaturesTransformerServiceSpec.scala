package com.foundaml.server.services

import org.scalatest._
import org.scalatest.Inside.inside

import com.foundaml.server.services.domain._
import com.foundaml.server.models.features._
import com.foundaml.server.models.features.transformers._
import com.foundaml.server.models.labels._
import com.foundaml.server.models.backends._
import com.foundaml.server.models._

import com.foundaml.server.utils._

import io.circe._
import io.circe.syntax._
import io.circe.parser._

import scalaz.zio.DefaultRuntime

class FeaturesTransformerServiceSpec extends FlatSpec with DefaultRuntime {

  val tfFakeJson = """
    {
      "signature_name": <string>,
      "context": {
        "<feature_name3>": <value>|<list>
        "<feature_name4>": <value>|<list>
      },

      "examples": [
        {
          "test1": 10,
          "test2": "toto",
        },
      ]
    }

"""



  val predictionsService = new PredictionsService()

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
          Json.eqJson.eqv(transformer.toJson(tfFeatures), expectedJson)
        )   
    }
  }
}
