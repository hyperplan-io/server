package com.foundaml.server.services

import org.scalatest._

import com.foundaml.server.services.domain._
import com.foundaml.server.models.features._
import com.foundaml.server.models.features.transformers._
import com.foundaml.server.models.labels._
import com.foundaml.server.models.backends._
import com.foundaml.server.models._

import com.foundaml.server.utils._

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

  it should "transforme features to a tensorflow classify compatible format" in {
    val features = Features(
      List(
        StringFeature("test instance"),
        IntFeature(1),
        FloatFeature(0.5f)
      )
    )


    val transformer = new TensorFlowFeaturesTransformer(
      List(
        "test",
        "toto"
      )
    )

    val transformedFeatures = transformer.transform(
      features
    )
    print(transformer.toJson(transformedFeatures).noSpaces)
  }
}
