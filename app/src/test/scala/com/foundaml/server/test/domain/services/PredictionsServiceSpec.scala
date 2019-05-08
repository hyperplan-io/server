package com.foundaml.server.test.domain.services

import java.util.UUID

import com.foundaml.server.domain.factories.ProjectFactory
import com.foundaml.server.domain._
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.errors.FeaturesValidationFailed
import com.foundaml.server.domain.services.PredictionsService
import org.scalatest._
import org.scalatest.Inside.inside
import com.foundaml.server.domain.models.features._
import com.foundaml.server.domain.repositories.{
  AlgorithmsRepository,
  PredictionsRepository,
  ProjectsRepository
}
import com.foundaml.server.infrastructure.streaming.{
  KinesisService,
  PubSubService
}
import com.foundaml.server.test.{
  AlgorithmGenerator,
  ProjectGenerator,
  TestDatabase
}
import cats.implicits._
import cats.effect.IO
import scala.concurrent.ExecutionContext

import scala.util.Try

class PredictionsServiceSpec extends FlatSpec with TestDatabase {

  val config = FoundaMLConfig(
    KinesisConfig(enabled = false, "predictionsStream", "examplesStream"),
    GCPConfig(
      "myProjectId",
      PubSubConfig(
        enabled = false,
        "myTopic"
      )
    ),
    DatabaseConfig(
      PostgreSqlConfig(
        "host",
        5432,
        "database",
        "username",
        "password"
      )
    )
  )
  val kinesisService: KinesisService =
    KinesisService("fake-region").unsafeRunSync()
  val pubSubService: PubSubService =
    PubSubService("myProjectId", "myTopic").unsafeRunSync()
  val projectsRepository = new ProjectsRepository()(xa)
  val algorithmsRepository = new AlgorithmsRepository()(xa)
  val predictionsRepository = new PredictionsRepository()(xa)

  val projectFactory = new ProjectFactory(
    projectsRepository,
    algorithmsRepository
  )
  val predictionsService: PredictionsService =
    new PredictionsService(
      projectsRepository,
      predictionsRepository,
      kinesisService,
      Some(pubSubService),
      projectFactory,
      config
    )

  it should "fail to execute predictions for an incorrect configuration" in {
    val features = List(
      StringFeature("test instance"),
      IntFeature(1),
      FloatFeature(0.5f)
    )

    val algorithmId = UUID.randomUUID().toString
    val project = ProjectGenerator.withLocalBackend(
      Some(List(AlgorithmGenerator.withLocalBackend(Some(algorithmId))))
    )

    val shouldThrow = Try(
      predictionsService
        .predictForClassificationProject(
          project,
          features,
          Some(algorithmId)
        )
        .unsafeRunSync()
    )
    inside(shouldThrow.toEither) {
      case Left(err) =>
        assert(
          err.getMessage
            .contains("The features do not match the configuration of project")
        )
    }

  }

  it should "validate int features" in {

    val goodConfig = FeaturesConfiguration(
      "id",
      List(
        FeatureConfiguration(
          "myInt",
          "Int",
          "an example of an int"
        )
      )
    )
    val features = List(IntFeature(1))
    assert(
      predictionsService.validateFeatures(
        goodConfig,
        features
      ) == true
    )

    val badConfig = FeaturesConfiguration(
      "id",
      List(
        FeatureConfiguration(
          "myInt",
          "int",
          "an example of an int"
        )
      )
    )
    assert(
      predictionsService.validateFeatures(
        badConfig,
        features
      ) == false
    )
  }

  it should "validate float features" in {

    val goodConfig = FeaturesConfiguration(
      "id",
      List(
        FeatureConfiguration(
          "myFloat",
          "Float",
          "an example of a float"
        )
      )
    )
    val features = List(FloatFeature(1.0f))
    assert(
      predictionsService.validateFeatures(
        goodConfig,
        features
      ) == true
    )

    val badConfig = FeaturesConfiguration(
      "id",
      List(
        FeatureConfiguration(
          "myFloat",
          "float",
          "an example of a float"
        )
      )
    )
    assert(
      predictionsService.validateFeatures(
        badConfig,
        features
      ) == false
    )
  }

  it should "validate string features" in {

    val goodConfig = FeaturesConfiguration(
      "id",
      List(
        FeatureConfiguration(
          "myString",
          "String",
          "an example of a string"
        )
      )
    )
    val features = List(StringFeature("hello"))
    assert(
      predictionsService.validateFeatures(
        goodConfig,
        features
      ) == true
    )

    val badConfig = FeaturesConfiguration(
      "id",
      List(
        FeatureConfiguration(
          "myString",
          "string",
          "an example of a string"
        )
      )
    )
    assert(
      predictionsService.validateFeatures(
        badConfig,
        features
      ) == false
    )
  }

  it should "validate int vector features" in {

    val goodConfig = FeaturesConfiguration(
      "id",
      List(
        FeatureConfiguration(
          "myIntVector",
          "IntVector",
          "an example of a int vector"
        )
      )
    )
    val features = List(IntVectorFeature(List(1, 2, 3)))
    assert(
      predictionsService.validateFeatures(
        goodConfig,
        features
      ) == true
    )

    val badConfig = FeaturesConfiguration(
      "id",
      List(
        FeatureConfiguration(
          "myIntVector",
          "intvector",
          "an example of a int vector"
        )
      )
    )
    assert(
      predictionsService.validateFeatures(
        badConfig,
        features
      ) == false
    )
  }

  it should "validate float vector features" in {

    val goodConfig = FeaturesConfiguration(
      "id",
      List(
        FeatureConfiguration(
          "myFloatVector",
          "FloatVector",
          "an example of a float vector"
        )
      )
    )
    val features = List(FloatVectorFeature(List(1.0f, 2.0f, 3.0f)))
    assert(
      predictionsService.validateFeatures(
        goodConfig,
        features
      ) == true
    )

    val badConfig = FeaturesConfiguration(
      "id",
      List(
        FeatureConfiguration(
          "myFloatVector",
          "floatvector",
          "an example of a float vector"
        )
      )
    )
    assert(
      predictionsService.validateFeatures(
        badConfig,
        features
      ) == false
    )
  }

  it should "validate string vector features" in {

    val goodConfig = FeaturesConfiguration(
      "id",
      List(
        FeatureConfiguration(
          "myStringVector",
          "StringVector",
          "an example of a string vector"
        )
      )
    )
    val features = List(StringVectorFeature(List("hello", "bye")))
    assert(
      predictionsService.validateFeatures(
        goodConfig,
        features
      ) == true
    )

    val badConfig = FeaturesConfiguration(
      "id",
      List(
        FeatureConfiguration(
          "myStringVector",
          "stringvector",
          "an example of a string vector"
        )
      )
    )
    assert(
      predictionsService.validateFeatures(
        badConfig,
        features
      ) == false
    )
  }

  it should "validate int vector2d features" in {

    val goodConfig = FeaturesConfiguration(
      "id",
      List(
        FeatureConfiguration(
          "myIntVector2d",
          "IntVector2d",
          "an example of a int vector2d"
        )
      )
    )
    val features = List(
      IntVector2dFeature(
        List(
          List(1, 2, 3),
          List(4, 5, 6)
        )
      )
    )
    assert(
      predictionsService.validateFeatures(
        goodConfig,
        features
      ) == true
    )

    val badConfig = FeaturesConfiguration(
      "id",
      List(
        FeatureConfiguration(
          "myIntVector2d",
          "intvector2d",
          "an example of a int vector2d"
        )
      )
    )
    assert(
      predictionsService.validateFeatures(
        badConfig,
        features
      ) == false
    )
  }

  it should "validate float vector2d features" in {

    val goodConfig = FeaturesConfiguration(
      "id",
      List(
        FeatureConfiguration(
          "myFloatVector2d",
          "FloatVector2d",
          "an example of a float vector2d"
        )
      )
    )
    val features = List(
      FloatVector2dFeature(
        List(
          List(1, 0f, 2.0f, 3.0f),
          List(4.0f, 5.0f, 6.0f)
        )
      )
    )
    assert(
      predictionsService.validateFeatures(
        goodConfig,
        features
      ) == true
    )

    val badConfig = FeaturesConfiguration(
      "id",
      List(
        FeatureConfiguration(
          "myFloatVector2d",
          "floatvector2d",
          "an example of a float vector2d"
        )
      )
    )
    assert(
      predictionsService.validateFeatures(
        badConfig,
        features
      ) == false
    )
  }

  it should "validate string vector2d features" in {

    val goodConfig = FeaturesConfiguration(
      "id",
      List(
        FeatureConfiguration(
          "myStringVector2d",
          "StringVector2d",
          "an example of a string vector2d"
        )
      )
    )
    val features = List(
      StringVector2dFeature(
        List(List("hello", "bye"))
      )
    )
    assert(
      predictionsService.validateFeatures(
        goodConfig,
        features
      ) == true
    )

    val badConfig = FeaturesConfiguration(
      "id",
      List(
        FeatureConfiguration(
          "myStringVector2d",
          "stringvector2d",
          "an example of a string vector2d"
        )
      )
    )
    assert(
      predictionsService.validateFeatures(
        badConfig,
        features
      ) == false
    )
  }

}
